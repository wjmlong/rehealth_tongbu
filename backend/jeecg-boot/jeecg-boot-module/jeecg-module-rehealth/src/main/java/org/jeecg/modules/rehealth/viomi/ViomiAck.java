package org.jeecg.modules.rehealth.viomi;

/**
 * Acknowledgement returned to the Viomi platform.
 *
 * <p>The Viomi active-report contract expects exactly {@code {"code":1,"msg":"操作成功"}} on success
 * and {@code {"code":0,"msg":"操作失败"}} on failure. Keep the JSON shape stable because the
 * platform uses the {@code code} field to mark a report as delivered.</p>
 */
public class ViomiAck {

    public int code;
    public String msg;

    public ViomiAck(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ViomiAck ok() {
        return new ViomiAck(1, "操作成功");
    }

    public static ViomiAck fail(String reason) {
        return new ViomiAck(0, "操作失败");
    }
}
