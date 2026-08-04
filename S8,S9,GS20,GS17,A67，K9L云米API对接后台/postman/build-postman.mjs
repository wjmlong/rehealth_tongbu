import { writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const outputDir = dirname(fileURLToPath(import.meta.url));
const schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";

const script = (type, lines) => ({
  listen: type,
  script: { type: "text/javascript", exec: lines },
});

const jsonBody = (value) => ({
  mode: "raw",
  raw: JSON.stringify(value, null, 2).replace(
    /"\{\{(userId|groupId|stepType)\}\}"/g,
    "{{$1}}",
  ),
  options: { raw: { language: "json" } },
});

const standardHeaders = [
  { key: "Content-Type", value: "application/json", type: "text" },
  { key: "Accept", value: "application/json", type: "text" },
  {
    key: "Authorization",
    value: "{{accessToken}}",
    type: "text",
    description: "云米要求 Header 与请求体同时携带 AccessToken。",
  },
];

const commonTests = [
  "pm.test('HTTP 状态码为 2xx', function () {",
  "  pm.expect(pm.response.code).to.be.within(200, 299);",
  "});",
  "pm.test('响应为 JSON', function () {",
  "  pm.response.to.be.json;",
  "});",
  "const data = pm.response.json();",
  "const code = data.Code !== undefined ? data.Code : data.code;",
  "pm.test('响应包含业务状态码', function () {",
  "  pm.expect(code).to.not.equal(undefined);",
  "});",
  "pm.test('业务状态码为文档中的成功值 0 或 1', function () {",
  "  pm.expect([0, 1]).to.include(Number(code));",
  "});",
  "console.log('Viomi response:', data);",
];

const commandTests = [
  "pm.test('HTTP 状态码为 2xx', function () {",
  "  pm.expect(pm.response.code).to.be.within(200, 299);",
  "});",
  "pm.test('响应为 JSON', function () { pm.response.to.be.json; });",
  "const data = pm.response.json();",
  "const code = Number(data.Code !== undefined ? data.Code : data.code);",
  "pm.test('响应包含业务状态码', function () {",
  "  pm.expect(code).to.not.equal(undefined);",
  "});",
  "pm.test('指令已受理', function () {",
  "  pm.expect([0, 1, 1803]).to.include(code);",
  "});",
  "console.log('Viomi command response:', data);",
];

const historyPreRequest = [
  "function utcText(date) {",
  "  return date.toISOString().slice(0, 19).replace('T', ' ');",
  "}",
  "const now = new Date();",
  "const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);",
  "pm.variables.set('beginTime', pm.environment.get('beginTime') || utcText(sevenDaysAgo));",
  "pm.variables.set('endTime', pm.environment.get('endTime') || utcText(now));",
];

const commandPreRequest = [
  "const seconds = Math.floor(Date.now() / 1000).toString();",
  "pm.variables.set('requestTimestamp', seconds);",
  "pm.variables.set('requestId', 'rh-' + seconds + '-' + Math.random().toString(16).slice(2, 10));",
];

function requestItem({ name, path, body, description, tests = commonTests, preRequest = [] }) {
  const event = [];
  if (preRequest.length) event.push(script("prerequest", preRequest));
  if (tests.length) event.push(script("test", tests));
  return {
    name,
    request: {
      method: "POST",
      header: standardHeaders,
      body: jsonBody(body),
      url: `{{baseUrl}}${path}`,
      description,
    },
    response: [],
    event,
  };
}

const historyBody = (extra = {}) => ({
  AccessToken: "{{accessToken}}",
  Imei: "{{imei}}",
  BeginTime: "{{beginTime}}",
  EndTime: "{{endTime}}",
  ...extra,
});

const commandBody = (commandVariable) => ({
  AccessToken: "{{accessToken}}",
  Imei: "{{imei}}",
  Time: "{{requestTimestamp}}",
  CommandCode: `{{${commandVariable}}}`,
  CommandValue: "",
  ReqId: "{{requestId}}",
});

const collection = {
  info: {
    _postman_id: "2973ac84-d5ec-4aa6-a138-b3a19aa13fa6",
    name: "云米 OpenAPI - ReHealth 手表数据测试",
    description:
      "面向 S8/S9/GS20/GS17/A67/K9L 等云米平台手表的只读查询与主动测量测试集合。先执行“获取 AccessToken”，脚本会保存 accessToken 和 userId。除 Token 外，云米文档要求 Authorization Header 和 JSON Body 同时携带 AccessToken。\n\n注意：云米在线帮助页示例通常以 Code=1 表示成功，而随项目交付的 V1.6.7 文档部分章节以 Code=0 表示成功；测试脚本兼容 0/1。sendcommand 还兼容 1803（指令已下发）。主动测量只表示命令被受理，结果需稍后通过相应历史/最新数据接口查询。",
    schema,
  },
  variable: [
    { key: "baseUrl", value: "https://openapi.miwitracker.com", type: "string" },
  ],
  item: [
    {
      name: "00 - 认证与设备",
      item: [
        {
          name: "01 获取 AccessToken（自动计算 MD5）",
          request: {
            method: "POST",
            header: [
              { key: "Content-Type", value: "application/json", type: "text" },
              { key: "Accept", value: "application/json", type: "text" },
            ],
            body: {
              mode: "raw",
              raw: '{\n  "Password": "{{password}}",\n  "AppId": {{appId}},\n  "Timestamp": {{timestamp}}\n}',
              options: { raw: { language: "json" } },
            },
            url: "{{baseUrl}}/api/token/get_token",
            description:
              "Password = MD5(AppKey + AppId + Timestamp)，32 位小写。预请求脚本自动生成秒级 Timestamp 和 Password；响应脚本自动保存 Result.AccessToken、Result.UserId 和 Result.Expire。",
          },
          response: [],
          event: [
            script("prerequest", [
              "function add32(a, b) { return (a + b) & 0xFFFFFFFF; }",
              "function cmn(q, a, b, x, s, t) {",
              "  a = add32(add32(a, q), add32(x, t));",
              "  return add32((a << s) | (a >>> (32 - s)), b);",
              "}",
              "function ff(a, b, c, d, x, s, t) { return cmn((b & c) | ((~b) & d), a, b, x, s, t); }",
              "function gg(a, b, c, d, x, s, t) { return cmn((b & d) | (c & (~d)), a, b, x, s, t); }",
              "function hh(a, b, c, d, x, s, t) { return cmn(b ^ c ^ d, a, b, x, s, t); }",
              "function ii(a, b, c, d, x, s, t) { return cmn(c ^ (b | (~d)), a, b, x, s, t); }",
              "function md5cycle(state, block) {",
              "  let [a, b, c, d] = state;",
              "  const [oa, ob, oc, od] = state;",
              "  a = ff(a,b,c,d,block[0],7,-680876936); d = ff(d,a,b,c,block[1],12,-389564586); c = ff(c,d,a,b,block[2],17,606105819); b = ff(b,c,d,a,block[3],22,-1044525330);",
              "  a = ff(a,b,c,d,block[4],7,-176418897); d = ff(d,a,b,c,block[5],12,1200080426); c = ff(c,d,a,b,block[6],17,-1473231341); b = ff(b,c,d,a,block[7],22,-45705983);",
              "  a = ff(a,b,c,d,block[8],7,1770035416); d = ff(d,a,b,c,block[9],12,-1958414417); c = ff(c,d,a,b,block[10],17,-42063); b = ff(b,c,d,a,block[11],22,-1990404162);",
              "  a = ff(a,b,c,d,block[12],7,1804603682); d = ff(d,a,b,c,block[13],12,-40341101); c = ff(c,d,a,b,block[14],17,-1502002290); b = ff(b,c,d,a,block[15],22,1236535329);",
              "  a = gg(a,b,c,d,block[1],5,-165796510); d = gg(d,a,b,c,block[6],9,-1069501632); c = gg(c,d,a,b,block[11],14,643717713); b = gg(b,c,d,a,block[0],20,-373897302);",
              "  a = gg(a,b,c,d,block[5],5,-701558691); d = gg(d,a,b,c,block[10],9,38016083); c = gg(c,d,a,b,block[15],14,-660478335); b = gg(b,c,d,a,block[4],20,-405537848);",
              "  a = gg(a,b,c,d,block[9],5,568446438); d = gg(d,a,b,c,block[14],9,-1019803690); c = gg(c,d,a,b,block[3],14,-187363961); b = gg(b,c,d,a,block[8],20,1163531501);",
              "  a = gg(a,b,c,d,block[13],5,-1444681467); d = gg(d,a,b,c,block[2],9,-51403784); c = gg(c,d,a,b,block[7],14,1735328473); b = gg(b,c,d,a,block[12],20,-1926607734);",
              "  a = hh(a,b,c,d,block[5],4,-378558); d = hh(d,a,b,c,block[8],11,-2022574463); c = hh(c,d,a,b,block[11],16,1839030562); b = hh(b,c,d,a,block[14],23,-35309556);",
              "  a = hh(a,b,c,d,block[1],4,-1530992060); d = hh(d,a,b,c,block[4],11,1272893353); c = hh(c,d,a,b,block[7],16,-155497632); b = hh(b,c,d,a,block[10],23,-1094730640);",
              "  a = hh(a,b,c,d,block[13],4,681279174); d = hh(d,a,b,c,block[0],11,-358537222); c = hh(c,d,a,b,block[3],16,-722521979); b = hh(b,c,d,a,block[6],23,76029189);",
              "  a = hh(a,b,c,d,block[9],4,-640364487); d = hh(d,a,b,c,block[12],11,-421815835); c = hh(c,d,a,b,block[15],16,530742520); b = hh(b,c,d,a,block[2],23,-995338651);",
              "  a = ii(a,b,c,d,block[0],6,-198630844); d = ii(d,a,b,c,block[7],10,1126891415); c = ii(c,d,a,b,block[14],15,-1416354905); b = ii(b,c,d,a,block[5],21,-57434055);",
              "  a = ii(a,b,c,d,block[12],6,1700485571); d = ii(d,a,b,c,block[3],10,-1894986606); c = ii(c,d,a,b,block[10],15,-1051523); b = ii(b,c,d,a,block[1],21,-2054922799);",
              "  a = ii(a,b,c,d,block[8],6,1873313359); d = ii(d,a,b,c,block[15],10,-30611744); c = ii(c,d,a,b,block[6],15,-1560198380); b = ii(b,c,d,a,block[13],21,1309151649);",
              "  a = ii(a,b,c,d,block[4],6,-145523070); d = ii(d,a,b,c,block[11],10,-1120210379); c = ii(c,d,a,b,block[2],15,718787259); b = ii(b,c,d,a,block[9],21,-343485551);",
              "  state[0] = add32(a, oa); state[1] = add32(b, ob); state[2] = add32(c, oc); state[3] = add32(d, od);",
              "}",
              "function md5block(text) {",
              "  const block = [];",
              "  for (let i = 0; i < 64; i += 4) block[i >> 2] = text.charCodeAt(i) + (text.charCodeAt(i + 1) << 8) + (text.charCodeAt(i + 2) << 16) + (text.charCodeAt(i + 3) << 24);",
              "  return block;",
              "}",
              "function md5state(text) {",
              "  let index; const length = text.length; const state = [1732584193, -271733879, -1732584194, 271733878];",
              "  for (index = 64; index <= length; index += 64) md5cycle(state, md5block(text.substring(index - 64, index)));",
              "  text = text.substring(index - 64); const tail = Array(16).fill(0);",
              "  for (index = 0; index < text.length; index++) tail[index >> 2] |= text.charCodeAt(index) << ((index % 4) << 3);",
              "  tail[index >> 2] |= 0x80 << ((index % 4) << 3);",
              "  if (index > 55) { md5cycle(state, tail); tail.fill(0); }",
              "  tail[14] = length * 8; md5cycle(state, tail); return state;",
              "}",
              "function hex32(value) {",
              "  let out = ''; const chars = '0123456789abcdef';",
              "  for (let j = 0; j < 4; j++) out += chars[(value >> (j * 8 + 4)) & 15] + chars[(value >> (j * 8)) & 15];",
              "  return out;",
              "}",
              "function md5(value) {",
              "  const bytes = unescape(encodeURIComponent(value));",
              "  return md5state(bytes).map(hex32).join('');",
              "}",
              "if (md5('abc') !== '900150983cd24fb0d6963f7d28e17f72') throw new Error('MD5 自检失败');",
              "const appKey = pm.environment.get('appKey');",
              "const appId = pm.environment.get('appId');",
              "if (!appKey || !appId || appId === '0') {",
              "  throw new Error('请先在 Environment 中填写 appKey 和 appId');",
              "}",
              "const timestamp = Math.floor(Date.now() / 1000).toString();",
              "const password = md5(appKey + appId + timestamp);",
              "pm.variables.set('timestamp', timestamp);",
              "pm.variables.set('password', password);",
            ]),
            script("test", [
              "pm.test('HTTP 状态码为 2xx', function () { pm.expect(pm.response.code).to.be.within(200, 299); });",
              "pm.test('响应为 JSON', function () { pm.response.to.be.json; });",
              "const data = pm.response.json();",
              "const code = data.Code !== undefined ? data.Code : data.code;",
              "pm.test('Token 请求业务成功', function () { pm.expect([0, 1]).to.include(Number(code)); });",
              "const result = data.Result || data.result;",
              "pm.test('响应包含 AccessToken', function () { pm.expect(result && result.AccessToken).to.be.a('string').and.not.empty; });",
              "if (result && result.AccessToken) {",
              "  pm.environment.set('accessToken', result.AccessToken);",
              "  if (result.UserId !== undefined) pm.environment.set('userId', String(result.UserId));",
              "  if (result.Expire !== undefined) pm.environment.set('tokenExpire', String(result.Expire));",
              "}",
            ]),
          ],
        },
        requestItem({
          name: "02 获取设备列表",
          path: "/api/devicelist/get_devicelist",
          body: {
            AccessToken: "{{accessToken}}",
            UserId: "{{userId}}",
            MapType: "{{mapType}}",
            GroupId: "{{groupId}}",
          },
          description:
            "获取 Token 对应账号下的设备。若 Environment 中 imei 为空，测试脚本会把 Result[0].Imei 保存为 imei。",
          tests: [
            ...commonTests,
            "if (!pm.environment.get('imei') && Array.isArray(data.Result) && data.Result[0] && data.Result[0].Imei) {",
            "  pm.environment.set('imei', String(data.Result[0].Imei));",
            "}",
          ],
        }),
        requestItem({
          name: "03 获取最新健康汇总",
          path: "/api/healthinfo/get_latest_healthinfo",
          body: { AccessToken: "{{accessToken}}", Imei: "{{imei}}" },
          description:
            "返回设备最近一次步数、心率、血压、睡眠、血氧和体温汇总。字段可能因设备型号/固件能力而为空。",
        }),
      ],
    },
    {
      name: "01 - 历史健康数据",
      event: [script("prerequest", historyPreRequest)],
      item: [
        requestItem({
          name: "01 心率历史",
          path: "/api/heartrate/get_heartrate_bytime",
          body: historyBody(),
          description: "结果字段：Result[].HeartRate、Result[].HrTime。BeginTime/EndTime 使用 UTC。",
        }),
        requestItem({
          name: "02 血压历史",
          path: "/api/bloodpressure/get_bloodpressure_bytime",
          body: historyBody(),
          description: "结果字段：Result[].Systolic、Result[].Diastolic、Result[].BpTime。消费级手表血压不能替代临床袖带血压。",
        }),
        requestItem({
          name: "03 血氧历史",
          path: "/api/bloodoxygen/get_bloodoxygen_bytime",
          body: historyBody(),
          description: "结果字段：Result[].BloodOxygen、Result[].BloodOxygenTime。",
        }),
        requestItem({
          name: "04 体温历史",
          path: "/api/Temperature/get_temperature_bytime",
          body: historyBody(),
          description: "结果字段：Result[].Temperature、Result[].TemperatureTime。官方在线帮助使用大写 Temperature 路径。",
        }),
        requestItem({
          name: "05 时间段步数汇总",
          path: "/api/steps/get_steps_bytime",
          body: historyBody({ StepType: "{{stepType}}" }),
          description:
            "结果字段：Result.Imei、Result.Steps、Result.Distance、Result.Calorie。当前在线模型含 StepType；默认 0，具体清零语义需与云米确认。",
        }),
        requestItem({
          name: "06 按天步数",
          path: "/api/steps/get_steps_forday",
          body: historyBody(),
          description: "结果字段：Items[].Date/Steps/Distance/Calorie，以及总计 Step/Distance/Calorie。",
        }),
        requestItem({
          name: "07 睡眠历史",
          path: "/api/sleep/get_sleep_bytime",
          body: historyBody(),
          description: "结果字段：Result[].TotalSleep、LighSleep（云米字段原拼写）、DeepSleep、SleepTime、Wake、EyeMove。",
        }),
        requestItem({
          name: "08 呼吸率历史",
          path: "/api/RespiRate/GetRespiRate",
          body: historyBody(),
          description: "结果字段：Result[].RespiratoryRate、Result[].UtcTime。仅支持具备对应能力的型号。",
        }),
      ],
    },
    {
      name: "02 - 主动测量（会向在线手表下发指令）",
      description:
        "谨慎执行。返回 0/1/1803 只表示命令成功或已下发，并不携带测量结果。等待设备上传后，再执行“最新健康汇总”或相应历史查询。命令码可能受具体型号和固件影响。",
      event: [script("prerequest", commandPreRequest)],
      item: [
        requestItem({
          name: "01 立即健康/心率（默认 9012）",
          path: "/api/command/sendcommand",
          body: commandBody("cmdHeartRate"),
          description: "文档将 9012 描述为“健康数据”或“测量心率”。请求成功后轮询最新健康/心率历史接口。",
          tests: commandTests,
        }),
        requestItem({
          name: "02 立即血压（默认 9510，需型号确认）",
          path: "/api/command/sendcommand",
          body: commandBody("cmdBloodPressure"),
          description: "V1.6.7 指令表记录 9510 为测量血压。执行前应向云米确认目标型号和固件支持。",
          tests: commandTests,
        }),
        requestItem({
          name: "03 立即血氧（默认 9511，需型号确认）",
          path: "/api/command/sendcommand",
          body: commandBody("cmdBloodOxygen"),
          description:
            "V1.6.7 同时出现 9511 与 9726 两种血氧相关定义，且 9726 也被描述为上传间隔。为避免误改设备配置，本集合默认 9511；执行前必须按具体型号向云米确认。",
          tests: commandTests,
        }),
        requestItem({
          name: "04 立即体温（默认 9111）",
          path: "/api/command/sendcommand",
          body: commandBody("cmdTemperature"),
          description: "V1.6.7 指令表记录 9111 为测量体温。请求成功后轮询体温历史接口。",
          tests: commandTests,
        }),
      ],
    },
    {
      name: "03 - 定位数据（敏感数据）",
      description: "定位数据属于敏感个人数据，仅测试已授权设备，不要在日志、截图或工单中暴露 IMEI、Token 和经纬度。",
      event: [script("prerequest", historyPreRequest)],
      item: [
        requestItem({
          name: "01 获取当前位置",
          path: "/api/location/get_location_info",
          body: { AccessToken: "{{accessToken}}", Imei: "{{imei}}", MapType: "{{mapType}}" },
          description: "返回最近位置、定位类型、设备状态、电量等信息。",
        }),
        requestItem({
          name: "02 获取历史轨迹",
          path: "/api/track/get_track_info",
          body: historyBody({ MapType: "{{mapType}}" }),
          description: "结果字段：Result[].GpsTime、Lat、Lng、PosType、Speed、Direction。时间使用 UTC。",
        }),
        requestItem({
          name: "03 请求立即定位（默认 0039）",
          path: "/api/command/sendcommand",
          body: commandBody("cmdLocation"),
          description: "向在线手表下发立即定位命令。命令被受理后再查询当前位置；响应本身不包含经纬度。",
          tests: commandTests,
          preRequest: commandPreRequest,
        }),
      ],
    },
  ],
};

const environment = {
  id: "64166cf9-ac0c-40ea-9702-9cfddf40752b",
  name: "云米 OpenAPI - 本地测试环境",
  values: [
    { key: "baseUrl", value: "https://openapi.miwitracker.com", type: "default", enabled: true },
    { key: "appId", value: "0", type: "default", enabled: true },
    { key: "appKey", value: "", type: "secret", enabled: true },
    { key: "accessToken", value: "", type: "secret", enabled: true },
    { key: "tokenExpire", value: "", type: "default", enabled: true },
    { key: "userId", value: "0", type: "default", enabled: true },
    { key: "imei", value: "", type: "secret", enabled: true },
    { key: "mapType", value: "Baidu", type: "default", enabled: true },
    { key: "groupId", value: "0", type: "default", enabled: true },
    { key: "stepType", value: "0", type: "default", enabled: true },
    { key: "beginTime", value: "", type: "default", enabled: true },
    { key: "endTime", value: "", type: "default", enabled: true },
    { key: "cmdHeartRate", value: "9012", type: "default", enabled: true },
    { key: "cmdBloodPressure", value: "9510", type: "default", enabled: true },
    { key: "cmdBloodOxygen", value: "9511", type: "default", enabled: true },
    { key: "cmdTemperature", value: "9111", type: "default", enabled: true },
    { key: "cmdLocation", value: "0039", type: "default", enabled: true },
  ],
  _postman_variable_scope: "environment",
  _postman_exported_at: "2026-08-04T00:00:00.000Z",
  _postman_exported_using: "Codex",
};

writeFileSync(
  join(outputDir, "Viomi-OpenAPI-ReHealth.postman_collection.json"),
  `${JSON.stringify(collection, null, 2)}\n`,
  "utf8",
);
writeFileSync(
  join(outputDir, "Viomi-OpenAPI-ReHealth.postman_environment.json"),
  `${JSON.stringify(environment, null, 2)}\n`,
  "utf8",
);
