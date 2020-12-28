package com.shanjupay.transaction.controller;

/**
 * @Title: project
 * @Package * @Description:     * @author CodingSir
 * @date 2020/12/2413:46
 */
public enum BrowserType {
    ALIPAY,//支付宝
    WECHAT,//微信
    PC_BROWSER,//pc端浏览器
    MOBILE_BROWSER; //手机端浏览器

    public static BrowserType valueOfUserAgent(String userAgent) {
        if (userAgent != null && userAgent.contains("AlipayClient")) {
            return BrowserType.ALIPAY;
        } else if (userAgent != null && userAgent.contains("MicroMessenger")) {
            return BrowserType.WECHAT;
        } else {
            return BrowserType.MOBILE_BROWSER;
        }

    }
}
