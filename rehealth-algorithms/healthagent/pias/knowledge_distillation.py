"""
Knowledge Distillation for PIAS Engine

Train a lightweight V2-like model using soft labels from V8 teacher model.
Goal: Transfer V8's knowledge (AUC 0.86) to V2-compatible model (16+ features).

Expected improvement: AUC 0.767 → 0.80-0.82
"""

import numpy as np
import pandas as pd
import joblib
from typing import Dict, List, Optional, Tuple
from pathlib import Path


class KnowledgeDistiller:
    """
    Knowledge Distillation trainer.

    Process:
    1. Load V8 teacher model (97 features, AUC 0.86)
    2. Generate soft labels (probability predictions) on training data
    3. Train V2 student model (40 features) using soft labels
    4. The student learns to mimic the teacher's predictions
    """

    def __init__(
        self,
        teacher_model_path: str,
        student_model_type: str = "catboost",
        temperature: float = 3.0,
        alpha: float = 0.7,
    ):
        """
        Parameters
        ----------
        teacher_model_path : str
            Path to V8 teacher model (97 features)
        student_model_type : str
            Type of student model: "catboost", "lightgbm", "xgboost"
        temperature : float
            Distillation temperature (higher = softer probabilities)
        alpha : float
            Balance between soft labels (alpha) and hard labels (1-alpha)
        """
        self.teacher_model_path = teacher_model_path
        self.student_model_type = student_model_type
        self.temperature = temperature
        self.alpha = alpha
        self.teacher_model = None
        self.student_model = None
        self.feature_engineer = None

    def load_teacher(self):
        """Load the V8 teacher model."""
        print(f"Loading teacher model from {self.teacher_model_path}...")
        self.teacher_model = joblib.load(self.teacher_model_path)
        print("Teacher model loaded successfully.")
        return self

    def generate_soft_labels(self, X_teacher: pd.DataFrame) -> np.ndarray:
        """
        Generate soft labels from teacher model.

        Parameters
        ----------
        X_teacher : pd.DataFrame
            Features in teacher format (97 features)

        Returns
        -------
        np.ndarray of soft labels (probabilities)
        """
        if self.teacher_model is None:
            raise ValueError("Teacher model not loaded. Call load_teacher() first.")

        # Get teacher predictions
        if hasattr(self.teacher_model, 'predict_proba'):
            proba = self.teacher_model.predict_proba(X_teacher)
            if proba.ndim == 2:
                proba = proba[:, 1]  # Class 1 probability

        # Apply temperature scaling for softer labels
        # This makes the teacher's predictions less extreme
        logits = np.log(np.clip(proba, 1e-7, 1 - 1e-7))
        soft_labels = 1 / (1 + np.exp(-logits / self.temperature))

        return soft_labels

    def train_student(
        self,
        X_student: pd.DataFrame,
        y_hard: np.ndarray,
        soft_labels: np.ndarray,
        eval_set: Optional[Tuple[pd.DataFrame, np.ndarray]] = None,
    ) -> "KnowledgeDistiller":
        """
        Train student model using combined hard and soft labels.

        Parameters
        ----------
        X_student : pd.DataFrame
            Features in student format (40 features)
        y_hard : np.ndarray
            True labels (0/1)
        soft_labels : np.ndarray
            Soft labels from teacher
        eval_set : tuple, optional
            (X_val, y_val) for validation
        """
        print(f"Training {self.student_model_type} student model...")

        # Combine hard and soft labels
        # y_combined = alpha * soft_labels + (1 - alpha) * hard_labels
        y_combined = self.alpha * soft_labels + (1 - self.alpha) * y_hard

        if self.student_model_type == "catboost":
            self._train_catboost(X_student, y_combined, y_hard, eval_set)
        elif self.student_model_type == "lightgbm":
            self._train_lightgbm(X_student, y_combined, y_hard, eval_set)
        elif self.student_model_type == "xgboost":
            self._train_xgboost(X_student, y_combined, y_hard, eval_set)
        else:
            raise ValueError(f"Unknown model type: {self.student_model_type}")

        print("Student model training complete.")
        return self

    def _train_catboost(self, X, y_combined, y_hard, eval_set):
        """Train CatBoost student model."""
        try:
            from catboost import CatBoostRegressor, Pool

            # Use CatBoostRegressor for soft labels (continuous values)
            # This avoids the binary/multiclass classification error
            model = CatBoostRegressor(
                iterations=500,
                learning_rate=0.05,
                depth=6,
                l2_leaf_reg=3,
                random_seed=42,
                verbose=100,
                eval_metric="RMSE",
            )

            train_pool = Pool(X, y_combined)

            if eval_set:
                X_val, y_val = eval_set
                val_pool = Pool(X_val, y_val)
                model.fit(train_pool, eval_set=val_pool, early_stopping_rounds=50)
            else:
                model.fit(train_pool)

            self.student_model = model

        except ImportError:
            print("CatBoost not installed, falling back to sklearn...")
            self._train_sklearn_fallback(X, y_combined)

    def _train_lightgbm(self, X, y_combined, y_hard, eval_set):
        """Train LightGBM student model."""
        try:
            import lightgbm as lgb

            train_data = lgb.Dataset(X, label=y_combined)

            params = {
                "objective": "binary",
                "metric": "binary_logloss",
                "learning_rate": 0.05,
                "num_leaves": 31,
                "feature_fraction": 0.8,
                "bagging_fraction": 0.8,
                "bagging_freq": 5,
                "verbose": -1,
                "seed": 42,
            }

            callbacks = [lgb.log_evaluation(100)]

            if eval_set:
                X_val, y_val = eval_set
                val_data = lgb.Dataset(X_val, y_val, reference=train_data)
                model = lgb.train(
                    params, train_data,
                    num_boost_round=500,
                    valid_sets=[val_data],
                    callbacks=[lgb.early_stopping(50)] + callbacks,
                )
            else:
                model = lgb.train(params, train_data, num_boost_round=500, callbacks=callbacks)

            self.student_model = model

        except ImportError:
            print("LightGBM not installed, falling back to sklearn...")
            self._train_sklearn_fallback(X, y_combined)

    def _train_xgboost(self, X, y_combined, y_hard, eval_set):
        """Train XGBoost student model."""
        try:
            import xgboost as xgb

            dtrain = xgb.DMatrix(X, label=y_combined)

            params = {
                "objective": "binary:logistic",
                "eval_metric": "logloss",
                "learning_rate": 0.05,
                "max_depth": 6,
                "subsample": 0.8,
                "colsample_bytree": 0.8,
                "seed": 42,
            }

            watchlist = [(dtrain, "train")]

            if eval_set:
                X_val, y_val = eval_set
                dval = xgb.DMatrix(X_val, label=y_val)
                watchlist.append((dval, "eval"))
                model = xgb.train(
                    params, dtrain,
                    num_boost_round=500,
                    evals=watchlist,
                    early_stopping_rounds=50,
                    verbose_eval=100,
                )
            else:
                model = xgb.train(params, dtrain, num_boost_round=500, evals=watchlist, verbose_eval=100)

            self.student_model = model

        except ImportError:
            print("XGBoost not installed, falling back to sklearn...")
            self._train_sklearn_fallback(X, y_combined)

    def _train_sklearn_fallback(self, X, y_combined):
        """Fallback to sklearn GradientBoosting."""
        from sklearn.ensemble import GradientBoostingClassifier

        model = GradientBoostingClassifier(
            n_estimators=300,
            learning_rate=0.05,
            max_depth=5,
            subsample=0.8,
            random_state=42,
        )
        model.fit(X, y_combined)
        self.student_model = model

    def save_student(self, path: str):
        """Save the trained student model."""
        if self.student_model is None:
            raise ValueError("Student model not trained yet")

        joblib.dump(self.student_model, path)
        print(f"Student model saved to {path}")

    def evaluate(
        self,
        X: pd.DataFrame,
        y_true: np.ndarray,
        dataset_name: str = "Test"
    ) -> Dict:
        """
        Evaluate student model performance.

        Returns
        -------
        dict with AUC, accuracy, and classification report
        """
        from sklearn.metrics import roc_auc_score, accuracy_score, classification_report

        if self.student_model is None:
            raise ValueError("Student model not trained yet")

        # Get predictions
        if hasattr(self.student_model, 'predict_proba'):
            y_proba = self.student_model.predict_proba(X)
            if y_proba.ndim == 2:
                y_proba = y_proba[:, 1]
        else:
            y_proba = self.student_model.predict(X)

        y_pred = (y_proba > 0.5).astype(int)

        auc = roc_auc_score(y_true, y_proba)
        acc = accuracy_score(y_true, y_pred)

        print(f"\n{dataset_name} Results:")
        print(f"  AUC: {auc:.4f}")
        print(f"  Accuracy: {acc:.4f}")
        print(f"\nClassification Report:")
        print(classification_report(y_true, y_pred))

        return {
            "auc": auc,
            "accuracy": acc,
            "y_proba": y_proba,
            "y_pred": y_pred,
        }


class DistillationPipeline:
    """
    End-to-end distillation pipeline.

    Usage:
        pipeline = DistillationPipeline(
            teacher_path="models/rehealth_v8_final.pkl",
            data_path="data/training_data.csv",
        )
        pipeline.run()
    """

    def __init__(
        self,
        teacher_path: str,
        data_path: str,
        output_dir: str = "models/distilled",
        student_type: str = "catboost",
        n_features: int = 25,
    ):
        self.teacher_path = teacher_path
        self.data_path = data_path
        self.output_dir = Path(output_dir)
        self.student_type = student_type
        self.n_features = n_features

    def run(self):
        """Run the full distillation pipeline."""
        from sklearn.model_selection import train_test_split

        print("=" * 60)
        print("PIAS Knowledge Distillation Pipeline")
        print("=" * 60)

        # 1. Load data
        print("\n[1/6] Loading data...")
        df = pd.read_csv(self.data_path)
        print(f"  Loaded {len(df)} samples")

        # 2. Split data
        print("\n[2/6] Splitting data...")
        train_df, test_df = train_test_split(df, test_size=0.2, random_state=42, stratify=df["label"])
        train_df, val_df = train_test_split(train_df, test_size=0.15, random_state=42, stratify=train_df["label"])
        print(f"  Train: {len(train_df)}, Val: {len(val_df)}, Test: {len(test_df)}")

        # 3. Feature engineering
        print("\n[3/6] Applying feature engineering...")
        fe = FeatureEngineer()

        # For teacher (97 features)
        X_train_teacher = train_df[fe.BASE_FEATURES]  # Teacher uses original features
        X_val_teacher = val_df[fe.BASE_FEATURES]
        X_test_teacher = test_df[fe.BASE_FEATURES]

        # For student (40 features)
        X_train_student = fe.transform(train_df[fe.BASE_FEATURES])
        X_val_student = fe.transform(val_df[fe.BASE_FEATURES])
        X_test_student = fe.transform(test_df[fe.BASE_FEATURES])

        y_train = train_df["label"].values
        y_val = val_df["label"].values
        y_test = test_df["label"].values

        print(f"  Student features: {X_train_student.shape[1]}")

        # 4. Load teacher and generate soft labels
        print("\n[4/6] Loading teacher model...")
        distiller = KnowledgeDistiller(
            teacher_model_path=self.teacher_path,
            student_model_type=self.student_type,
            temperature=3.0,
            alpha=0.7,
        )
        distiller.load_teacher()

        print("\n[5/6] Generating soft labels from teacher...")
        soft_labels_train = distiller.generate_soft_labels(X_train_teacher)
        print(f"  Soft labels range: [{soft_labels_train.min():.3f}, {soft_labels_train.max():.3f}]")

        # 5. Train student
        print("\n[6/6] Training student model...")
        distiller.train_student(
            X_student=X_train_student,
            y_hard=y_train,
            soft_labels=soft_labels_train,
            eval_set=(X_val_student, y_val),
        )

        # 6. Evaluate
        print("\n" + "=" * 60)
        print("Evaluation Results")
        print("=" * 60)

        train_results = distiller.evaluate(X_train_student, y_train, "Train")
        val_results = distiller.evaluate(X_val_student, y_val, "Validation")
        test_results = distiller.evaluate(X_test_student, y_test, "Test")

        # 7. Save model
        self.output_dir.mkdir(parents=True, exist_ok=True)
        model_path = self.output_dir / f"rehealth_v2_distilled_{self.student_type}.pkl"
        distiller.save_student(str(model_path))

        # Save feature list
        feature_path = self.output_dir / "distilled_features.txt"
        with open(feature_path, "w") as f:
            for feat in X_train_student.columns:
                f.write(f"{feat}\n")

        print(f"\n✅ Model saved to: {model_path}")
        print(f"✅ Features saved to: {feature_path}")

        return {
            "model_path": str(model_path),
            "train_auc": train_results["auc"],
            "val_auc": val_results["auc"],
            "test_auc": test_results["auc"],
        }


# ─────────────────────────────────────────────
# CLI entry point
# ─────────────────────────────────────────────

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="PIAS Knowledge Distillation")
    parser.add_argument("--teacher", required=True, help="Path to V8 teacher model")
    parser.add_argument("--data", required=True, help="Path to training data CSV")
    parser.add_argument("--output", default="models/distilled", help="Output directory")
    parser.add_argument("--student-type", default="catboost", choices=["catboost", "lightgbm", "xgboost"])
    parser.add_argument("--n-features", type=int, default=25, help="Number of features to select")

    args = parser.parse_args()

    pipeline = DistillationPipeline(
        teacher_path=args.teacher,
        data_path=args.data,
        output_dir=args.output,
        student_type=args.student_type,
        n_features=args.n_features,
    )

    results = pipeline.run()
    print(f"\nFinal Results: {results}")
