package com.macro.mall.ai.util;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.listener.SaTokenEventCenter;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpLogic;

/**
 * 前台商城用户登录认证工具类（mall-ai 内部独立鉴权用）
 * 与 mall-portal 的 StpMemberUtil 共享 TYPE + Redis Session
 */
public class StpMemberUtil {

    private StpMemberUtil() {}

    public static final String TYPE = "memberLogin";

    public static StpLogic stpLogic = new StpLogicJwtForSimple(TYPE);

    public static void setStpLogic(StpLogic newStpLogic) {
        stpLogic = newStpLogic;
        SaManager.putStpLogic(newStpLogic);
        SaTokenEventCenter.doSetStpLogic(stpLogic);
    }

    public static StpLogic getStpLogic() {
        return stpLogic;
    }

    public static String getTokenName() {
        return stpLogic.getTokenName();
    }

    public static void setTokenValue(String tokenValue) {
        stpLogic.setTokenValue(tokenValue);
    }

    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }

    public static SaTokenInfo getTokenInfo() {
        return stpLogic.getTokenInfo();
    }

    public static void login(Object id) {
        stpLogic.login(id);
    }

    public static void login(Object id, long timeout) {
        stpLogic.login(id, timeout);
    }

    public static void logout() {
        stpLogic.logout();
    }

    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    public static void checkLogin() {
        stpLogic.checkLogin();
    }

    public static Object getLoginId() {
        return stpLogic.getLoginId();
    }

    public static Object getLoginIdDefaultNull() {
        return stpLogic.getLoginIdDefaultNull();
    }

    public static long getLoginIdAsLong() {
        return stpLogic.getLoginIdAsLong();
    }

    public static SaSession getSession() {
        return stpLogic.getSession();
    }

    public static SaSession getSession(boolean isCreate) {
        return stpLogic.getSession(isCreate);
    }

    public static SaSession getTokenSession() {
        return stpLogic.getTokenSession();
    }
}
