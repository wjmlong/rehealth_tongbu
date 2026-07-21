# -*- coding: utf-8 -*-
"""
睿禾全维健康风险预测算法 V2.0 - 官网对齐版
数据来源：NHANES 2015-2016
目标特征：完全对齐官网16个表单字段
输出模型：rehealth_v2_final.pkl（可直接部署）

官网16字段对应：
  age, gender, bmi, sbp, dbp,
  fasting_glucose, total_cholesterol, ldl, hdl, triglycerides,
  exercise_days, smoking, drinking,
  diabetes_history, hypertension_history, family_history
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, StratifiedKFold
from sklearn.metrics import roc_auc_score, classification_report
from imblearn.over_sampling import SMOTE
import optuna
from catboost import CatBoostClassifier
import joblib
import matplotlib.pyplot as plt
import warnings
warnings.filterwarnings('ignore')

optuna.logging.set_verbosity(optuna.logging.WARNING)

DATA_DIR = 'NHANES'  # 相对路径，脚本和NHANES文件夹同级

print("=" * 70)
print("睿禾全维健康风险预测算法 V2.0 — 官网对齐版")
print("特征：完全匹配官网16个表单字段")
print("=" * 70)

# ═══════════════════════════════════════════════════════
# 1. 读取 NHANES 数据
# ═══════════════════════════════════════════════════════
print("\n📁 读取 NHANES 2015-2016 数据...")

demo   = pd.read_sas(f'{DATA_DIR}/DEMO_I.XPT',   format='xport', encoding='latin-1')
bmx    = pd.read_sas(f'{DATA_DIR}/BMX_I.XPT',    format='xport', encoding='latin-1')
bpx    = pd.read_sas(f'{DATA_DIR}/BPX_I.XPT',    format='xport', encoding='latin-1')
bpq    = pd.read_sas(f'{DATA_DIR}/BPQ_I.XPT',    format='xport', encoding='latin-1')  # 高血压问卷
glu    = pd.read_sas(f'{DATA_DIR}/GLU_I.XPT',    format='xport', encoding='latin-1')
tchol  = pd.read_sas(f'{DATA_DIR}/TCHOL_I.XPT',  format='xport', encoding='latin-1')
hdl_df = pd.read_sas(f'{DATA_DIR}/HDL_I.XPT',    format='xport', encoding='latin-1')
trigly = pd.read_sas(f'{DATA_DIR}/TRIGLY_I.XPT', format='xport', encoding='latin-1')  # 含LDL
smq    = pd.read_sas(f'{DATA_DIR}/SMQ_I.XPT',    format='xport', encoding='latin-1')
alq    = pd.read_sas(f'{DATA_DIR}/ALQ_I.XPT',    format='xport', encoding='latin-1')
diq    = pd.read_sas(f'{DATA_DIR}/DIQ_I.XPT',    format='xport', encoding='latin-1')
paq    = pd.read_sas(f'{DATA_DIR}/PAQ_I.XPT',    format='xport', encoding='latin-1')
mcq    = pd.read_sas(f'{DATA_DIR}/MCQ_I.XPT',    format='xport', encoding='latin-1')

print("✅ 所有文件读取完成")

# ═══════════════════════════════════════════════════════
# 2. 合并数据
# ═══════════════════════════════════════════════════════
print("\n🔗 合并数据...")

df = demo[['SEQN']].copy()
for other in [bmx, bpx, bpq, glu, tchol, hdl_df, trigly, smq, alq, diq, paq, mcq]:
    df = pd.merge(df, other, on='SEQN', how='left')

print(f"合并后总行数: {len(df)}")

# ═══════════════════════════════════════════════════════
# 3. 提取并重命名为官网字段名
# ═══════════════════════════════════════════════════════
print("\n🔍 提取特征（对齐官网字段名）...")

feat = pd.DataFrame()
feat['SEQN'] = df['SEQN']

# ── 基础人口学 ──────────────────────────────────────────
feat['age']    = demo['RIDAGEYR']
feat['gender'] = (demo['RIAGENDR'] == 1).astype(int)   # 1=男, 0=女

# ── 体格测量 ────────────────────────────────────────────
feat['bmi']    = df['BMXBMI']

# ── 血压（取第一次测量值） ────────────────────────────
feat['sbp'] = df['BPXSY1']   # 收缩压
feat['dbp'] = df['BPXDI1']   # 舒张压

# ── 血检 ────────────────────────────────────────────────
feat['fasting_glucose']   = df['LBXGLU']    # 空腹血糖 mg/dL → 转换见下方
feat['total_cholesterol'] = df['LBXTC']     # 总胆固醇 mg/dL
feat['hdl']               = df['LBDHDD']    # HDL mg/dL
feat['ldl']               = df['LBDLDL']    # LDL mg/dL（来自TRIGLY文件）
feat['triglycerides']     = df['LBXTR']     # 甘油三酯 mg/dL

# 单位换算：mg/dL → mmol/L（与官网表单一致）
feat['fasting_glucose']   = feat['fasting_glucose']   * 0.0555
feat['total_cholesterol'] = feat['total_cholesterol'] * 0.02586
feat['hdl']               = feat['hdl']               * 0.02586
feat['ldl']               = feat['ldl']               * 0.02586
feat['triglycerides']     = feat['triglycerides']     * 0.01129

# ── 生活习惯 ────────────────────────────────────────────
# 吸烟：SMQ040=1(每天), 2(部分天) → 当前吸烟者
feat['smoking'] = 0
if 'SMQ040' in df.columns:
    feat['smoking'] = df['SMQ040'].isin([1, 2]).astype(int)

# 饮酒：ALQ130 过去12个月平均每天几杯，>=1 为饮酒者
feat['drinking'] = 0
if 'ALQ130' in df.columns:
    feat['drinking'] = (df['ALQ130'] >= 1).astype(int)

# 运动天数/周：PAD660=每周剧烈运动天数，PAD675=中等强度
# 取两者中较大值，限制在0-7
feat['exercise_days'] = 0
if 'PAD660' in df.columns and 'PAD675' in df.columns:
    vig = pd.to_numeric(df['PAD660'], errors='coerce').clip(0, 7).fillna(0)
    mod = pd.to_numeric(df['PAD675'], errors='coerce').clip(0, 7).fillna(0)
    feat['exercise_days'] = vig.combine(mod, max).astype(int)
elif 'PAQ605' in df.columns:
    # 备用：PAQ605=1表示有剧烈运动
    feat['exercise_days'] = (df['PAQ605'] == 1).astype(int) * 3  # 保守估计3天

# ── 疾病史 ──────────────────────────────────────────────
# 糖尿病史：DIQ010=1(Yes)
feat['diabetes_history'] = 0
if 'DIQ010' in df.columns:
    feat['diabetes_history'] = (df['DIQ010'] == 1).astype(int)

# 高血压史：BPQ020=1(曾被告知有高血压)
feat['hypertension_history'] = 0
if 'BPQ020' in df.columns:
    feat['hypertension_history'] = (df['BPQ020'] == 1).astype(int)

# 家族史（心脏病/中风家族史）：MCQ300B=1(心脏病直系亲属) 或 MCQ300C=1(中风)
feat['family_history'] = 0
fh_cols = ['MCQ300B', 'MCQ300C']
for col in fh_cols:
    if col in df.columns:
        feat['family_history'] = feat['family_history'] | (df[col] == 1).astype(int)

print("✅ 特征提取完成")

# ═══════════════════════════════════════════════════════
# 4. 构建标签（心血管事件：心脏病 OR 中风，不含高血压）
# 高血压是预测因子（特征），不是预测目标（标签）
# ═══════════════════════════════════════════════════════
print("\n🏷️ 构建标签...")

label = pd.Series(0, index=df.index)

# 冠心病/心绞痛 MCQ160B
# 心脏病发作 MCQ160C
# 充血性心衰 MCQ160D
# 冠状动脉病 MCQ160E
heart_cols = ['MCQ160B', 'MCQ160C', 'MCQ160D', 'MCQ160E']
for col in heart_cols:
    if col in df.columns:
        label = label | (df[col] == 1).astype(int)

# 中风 MCQ160F
if 'MCQ160F' in df.columns:
    label = label | (df['MCQ160F'] == 1).astype(int)

# ✅ BPQ020（高血压）不进标签，留作特征 hypertension_history

feat['label'] = label.values
print(f"标签分布: 正例 {feat['label'].sum()} / 总 {len(feat)}")
print(f"正例比例: {feat['label'].mean():.2%}")

# ═══════════════════════════════════════════════════════
# 5. 数据清洗
# ═══════════════════════════════════════════════════════
print("\n🧹 数据清洗...")

# 只保留年龄 18-100
feat = feat[(feat['age'] >= 18) & (feat['age'] <= 100)].copy()

# 范围过滤（去掉明显异常值）
filters = {
    'bmi':              (15,  60),
    'sbp':              (60,  250),
    'dbp':              (40,  150),
    'fasting_glucose':  (2.0, 35.0),
    'total_cholesterol':(2.0, 20.0),
    'hdl':              (0.3, 5.0),
    'ldl':              (0.5, 10.0),
    'triglycerides':    (0.2, 20.0),
}
for col, (lo, hi) in filters.items():
    feat[col] = feat[col].where(feat[col].between(lo, hi))

# 缺失值：中位数填补（连续），众数填补（分类）
CONT_COLS = ['age', 'bmi', 'sbp', 'dbp', 'fasting_glucose',
             'total_cholesterol', 'ldl', 'hdl', 'triglycerides', 'exercise_days']
CAT_COLS  = ['gender', 'smoking', 'drinking',
             'diabetes_history', 'hypertension_history', 'family_history']

for col in CONT_COLS:
    feat[col] = feat[col].fillna(feat[col].median())

for col in CAT_COLS:
    feat[col] = feat[col].fillna(feat[col].mode()[0]).astype(int)

# 丢弃标签缺失行
feat = feat.dropna(subset=['label'])
print(f"清洗后样本数: {len(feat)}")

# ═══════════════════════════════════════════════════════
# 6. 定义官网16个特征（严格顺序）
# ═══════════════════════════════════════════════════════

# ⚠️ 这个顺序必须和部署时的输入顺序完全一致
FEATURE_COLS = [
    'age', 'gender', 'bmi', 'sbp', 'dbp',
    'fasting_glucose', 'total_cholesterol', 'ldl', 'hdl', 'triglycerides',
    'exercise_days', 'smoking', 'drinking',
    'diabetes_history', 'hypertension_history', 'family_history'
]

CAT_FEATURE_NAMES = ['gender', 'smoking', 'drinking',
                     'diabetes_history', 'hypertension_history', 'family_history']

X = feat[FEATURE_COLS].copy()
y = feat['label'].copy()

print(f"\n✅ 最终特征列（{len(FEATURE_COLS)}个）: {FEATURE_COLS}")
print(f"✅ 分类特征: {CAT_FEATURE_NAMES}")
print(f"✅ 样本数: {len(X)}, 正例比例: {y.mean():.2%}")

# ═══════════════════════════════════════════════════════
# 7. 划分数据集
# ═══════════════════════════════════════════════════════
print("\n📊 划分数据集...")
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)
print(f"训练集: {len(X_train)}, 测试集: {len(X_test)}")

# ═══════════════════════════════════════════════════════
# 8. SMOTE 过采样
# ═══════════════════════════════════════════════════════
print("\n⚖️ SMOTE 过采样...")
smote = SMOTE(random_state=42, k_neighbors=5)
X_train_res, y_train_res = smote.fit_resample(X_train, y_train)
print(f"SMOTE后: {len(X_train_res)} 样本, 正例: {y_train_res.mean():.2%}")

# 确保分类特征为整数
for col in CAT_FEATURE_NAMES:
    X_train_res[col] = X_train_res[col].astype(int)
    X_test[col] = X_test[col].astype(int)

# ═══════════════════════════════════════════════════════
# 9. Optuna 调参（用列名传分类特征，更稳健）
# ═══════════════════════════════════════════════════════
print("\n🎯 Optuna 调参 (20 trials)...")

def objective(trial):
    params = {
        'iterations':    trial.suggest_int('iterations', 300, 700),
        'learning_rate': trial.suggest_float('learning_rate', 0.01, 0.15, log=True),
        'depth':         trial.suggest_int('depth', 4, 8),
        'l2_leaf_reg':   trial.suggest_int('l2_leaf_reg', 1, 10),
        'subsample':     trial.suggest_float('subsample', 0.6, 1.0),
        'colsample_bylevel': trial.suggest_float('colsample_bylevel', 0.6, 1.0),
        'random_seed':   42,
        'verbose':       False,
        'cat_features':  CAT_FEATURE_NAMES,  # ✅ 用列名，不用索引
    }
    model = CatBoostClassifier(**params)
    model.fit(X_train_res, y_train_res,
              eval_set=(X_test, y_test),
              verbose=False, early_stopping_rounds=30)
    y_proba = model.predict_proba(X_test)[:, 1]
    return roc_auc_score(y_test, y_proba)

study = optuna.create_study(
    direction='maximize',
    sampler=optuna.samplers.TPESampler(seed=42)
)
study.optimize(objective, n_trials=20, show_progress_bar=True)

print(f"\n✅ 最佳 AUC: {study.best_value:.4f}")
print(f"✅ 最佳参数: {study.best_params}")

# ═══════════════════════════════════════════════════════
# 10. 训练最终模型
# ═══════════════════════════════════════════════════════
print("\n🏆 训练最终模型...")

best_params = study.best_params.copy()
best_params.update({
    'random_seed':  42,
    'verbose':      100,
    'cat_features': CAT_FEATURE_NAMES,
})

final_model = CatBoostClassifier(**best_params)
final_model.fit(
    X_train_res, y_train_res,
    eval_set=(X_test, y_test),
    early_stopping_rounds=50
)

# ═══════════════════════════════════════════════════════
# 11. 评估
# ═══════════════════════════════════════════════════════
print("\n📈 评估结果...")

y_proba = final_model.predict_proba(X_test)[:, 1]
y_pred  = final_model.predict(X_test)
auc     = roc_auc_score(y_test, y_proba)

print(f"\nAUC: {auc:.4f}")
print(classification_report(y_test, y_pred, target_names=['未发病', '发病']))

# ═══════════════════════════════════════════════════════
# 12. 保存模型和元数据（部署用）
# ═══════════════════════════════════════════════════════
print("\n💾 保存模型...")

joblib.dump(final_model,    'rehealth_v2_final.pkl')
joblib.dump(FEATURE_COLS,   'feature_cols_v2.pkl')
joblib.dump(CAT_FEATURE_NAMES, 'cat_cols_v2.pkl')

print("✅ 已保存:")
print("   rehealth_v2_final.pkl   ← 部署用模型")
print("   feature_cols_v2.pkl     ← 特征列顺序")
print("   cat_cols_v2.pkl         ← 分类特征列名")

# 保存一份模型元信息（方便核查）
import json
meta = {
    "model_version": "v2.0",
    "auc": round(auc, 4),
    "feature_cols": FEATURE_COLS,
    "cat_features": CAT_FEATURE_NAMES,
    "n_features": len(FEATURE_COLS),
    "data_source": "NHANES 2015-2016",
    "unit_note": "血糖/血脂均为mmol/L，血压为mmHg，BMI为kg/m²"
}
with open('model_meta_v2.json', 'w', encoding='utf-8') as f:
    json.dump(meta, f, ensure_ascii=False, indent=2)
print("   model_meta_v2.json      ← 模型元信息")

# ═══════════════════════════════════════════════════════
# 13. 特征重要性图
# ═══════════════════════════════════════════════════════
importance_df = pd.DataFrame({
    'feature': FEATURE_COLS,
    'importance': final_model.feature_importances_
}).sort_values('importance', ascending=False)

print("\nTop 16 特征重要性:")
print(importance_df.to_string(index=False))

plt.figure(figsize=(10, 7))
colors = ['#ef4444' if i < 5 else '#f59e0b' if i < 10 else '#22c55e'
          for i in range(len(importance_df))]
plt.barh(importance_df['feature'][::-1], importance_df['importance'][::-1], color=colors[::-1])
plt.xlabel('Feature Importance')
plt.title(f'ReHealth Core V2.0 — 特征重要性\nAUC={auc:.4f} | NHANES 2015-2016')
plt.tight_layout()
plt.savefig('feature_importance_v2.png', dpi=150)
print("\n✅ feature_importance_v2.png 已保存")

# ═══════════════════════════════════════════════════════
# 14. ROC 曲线
# ═══════════════════════════════════════════════════════
from sklearn.metrics import roc_curve
fpr, tpr, _ = roc_curve(y_test, y_proba)

plt.figure(figsize=(7, 6))
plt.plot(fpr, tpr, color='#3b82f6', linewidth=2.5,
         label=f'ReHealth Core V2.0 (AUC = {auc:.3f})')
plt.plot([0, 1], [0, 1], 'k--', linewidth=1)
plt.fill_between(fpr, tpr, alpha=0.1, color='#3b82f6')
plt.xlabel('假阳性率 (1 - Specificity)')
plt.ylabel('真阳性率 (Sensitivity)')
plt.title('ROC曲线 — 睿禾心血管风险预测模型 V2.0')
plt.legend(loc='lower right')
plt.grid(True, alpha=0.3)
plt.tight_layout()
plt.savefig('roc_curve_v2.png', dpi=150)
print("✅ roc_curve_v2.png 已保存")

print("\n" + "=" * 70)
print(f"🎉 训练完成！最终 AUC: {auc:.4f}")
print(f"📦 部署文件: rehealth_v2_final.pkl")
print("=" * 70)
