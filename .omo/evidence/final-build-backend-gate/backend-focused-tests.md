# Backend focused test gate

- Invocation (WSL): `cd /mnt/d/rehealthAI/backend/jeecg-boot && mvn -pl jeecg-boot-module/jeecg-module-rehealth -am -Dtest='*Attribution*,*CvdFeatureVectorDto*' -Dsurefire.failIfNoSpecifiedTests=false test`
- Result: `BUILD SUCCESS`; reactor modules `JEECG BOOT`, `jeecg-boot-base-core`, `jeecg-boot-module`, `jeecg-module-rehealth` all succeeded; total time `05:12 min`.
- Focused tests: 5 run, 0 failures, 0 errors, 0 skipped.
- Surefire reports:
  - `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/surefire-reports/org.jeecg.modules.rehealth.mobile.controller.ReHealthMobileControllerAttributionContractTest.txt` (1 pass)
  - `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/surefire-reports/org.jeecg.modules.rehealth.mobile.dto.CvdFeatureVectorDtoJacksonBindingTest.txt` (2 passes)
  - `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/surefire-reports/org.jeecg.modules.rehealth.model.impl.HttpModelServiceClientAttributionTest.txt` (2 passes)

