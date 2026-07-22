from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from api.routers import patients, simulation, records, pias, insurance, pias_jeecg

app = FastAPI(
    title="ReHealth AI API - PIAS Engine",
    description="PIAS (Predict, Intervene, Attribute, Settle) 心血管疾病风险预测与干预归因平台",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(patients.router, prefix="/api/patients", tags=["患者管理"])
app.include_router(simulation.router, prefix="/api/simulation", tags=["健康模拟"])
app.include_router(records.router, prefix="/api/records", tags=["每日记录"])
app.include_router(pias.router, prefix="/api/pias", tags=["PIAS引擎"])
app.include_router(insurance.router, prefix="/api/insurance", tags=["保险服务"])
app.include_router(pias_jeecg.router, prefix="/api/pias/v2", tags=["PIAS-JeecgBoot兼容"])

@app.get("/")
def root():
    return {"status": "ok", "message": "ReHealth AI API 运行中"}

@app.get("/health")
def health():
    return {"status": "healthy"}
