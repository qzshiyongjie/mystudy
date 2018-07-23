package vip.firework.bean;

public class Message {
    private static final String DEFAULT_SUEECESS_CODE="0";
    private static final String DEFAULT_ERROR_CODE="1";
    private String code;
    private String msg;
    private Object data;
    private boolean success;

    public static Message defaultSuccess(String msg,Object data){
        Message message = defaultMessage(DEFAULT_SUEECESS_CODE, msg, data);
        return message;
    }

    public static Message defaultSuccess(Object data){
       return defaultSuccess("成功",data);
    }

    public static Message defaultError(String code,String msg,Object data){
        Message message = defaultMessage(code, msg, data);
        return message;
    }

    private static Message defaultMessage(String code, String msg, Object data) {
        Message message = new Message();
        message.setMsg(msg);
        message.setCode(code);
        message.setData(data);
        return message;
    }

    public static Message defaultError(String code,String msg){
        return defaultError(code,msg,"");
    }
    public static Message defaultError(String message){
        return defaultError(DEFAULT_ERROR_CODE,message,"");
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
