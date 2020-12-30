package com.shanjupay.paymentagent.config;

import com.github.wxpay.sdk.IWXPayDomain;
import com.github.wxpay.sdk.WXPayConfig;
import com.github.wxpay.sdk.WXPayConstants;
import com.shanjupay.paymentagent.api.conf.WXConfigParam;
import org.apache.dubbo.common.utils.Assert;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * @Title: project
 * @Package * @Description:     * @author CodingSir
 * @date 2020/12/309:01
 */
public class WXSDKConfig extends WXPayConfig {
    private WXConfigParam param;

    public WXSDKConfig(WXConfigParam param) {
        Assert.notNull(param, "微信支付参数不能为空");
        this.param = param;
    }

    public WXConfigParam getParam() {
        return param;
    }
    @Override
    public String getAppID() {
        return param.getAppId();
    }

    @Override
    protected String getMchID() {
        return param.getMchId();
    }

    @Override
    protected String getKey() {
        return param.getKey();
    }


    public String getAppSecret() {
        return param.getAppSecret();
    }
    @Override
    public InputStream getCertStream() {
        return null;
    }
    @Override
    public int getHttpConnectTimeoutMs() {
        return 8000;
    }

    @Override
    public int getHttpReadTimeoutMs() {
        return 10000;
    }

    @Override
    protected IWXPayDomain getWXPayDomain() {
        return new IWXPayDomain() {
            @Override
            public void report(String s, long l, Exception e) {

            }

            @Override
            public DomainInfo getDomain(WXPayConfig wxPayConfig) {//api.mch.weixin.qq.com
                return new DomainInfo(WXPayConstants.DOMAIN_API, true);
            }
        };
    }
}
