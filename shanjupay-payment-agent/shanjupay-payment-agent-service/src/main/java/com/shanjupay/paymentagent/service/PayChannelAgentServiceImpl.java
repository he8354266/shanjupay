package com.shanjupay.paymentagent.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;

/**
 * @Title: project
 * @Package * @Description:     * @author CodingSir
 * @date 2020/12/249:27
 */
@Service
@Slf4j
public class PayChannelAgentServiceImpl implements PayChannelAgentService {
    /**
     * 调用支付宝的下单接口
     *
     * @param aliConfigParam 支付渠道配置的参数（配置的支付宝的必要参数）
     * @param alipayBean     业务参数（商户订单号，订单标题，订单描述,,）
     * @return 统一返回PaymentResponseDTO
     */
    @Override
    public PaymentResponseDTO createPayOrderByAliWAP(AliConfigParam aliConfigParam, AlipayBean alipayBean) throws BusinessException {

        String url = aliConfigParam.getUrl();//支付宝接口网关地址
        String appId = aliConfigParam.getAppId();//支付宝应用id
        String rsaPrivateKey = aliConfigParam.getRsaPrivateKey();//应用私钥
        String format = aliConfigParam.getFormat();//json格式
        String charest = aliConfigParam.getCharest();//编码
        String alipayPublicKey = aliConfigParam.getAlipayPublicKey();//支付宝公钥
        String signtype = aliConfigParam.getSigntype();//签名算法
        String returnUrl = aliConfigParam.getReturnUrl();//支付成功跳转的url
        String notifyUrl = aliConfigParam.getNotifyUrl();//支付结果异步通知的url

        //构造sdk的客户端对象
        AlipayClient alipayClient = new DefaultAlipayClient(url, appId, rsaPrivateKey, format, charest, alipayPublicKey, signtype);
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(alipayBean.getOutTradeNo());//商户的订单，就是闪聚平台的订单
        model.setTotalAmount(alipayBean.getTotalAmount());//订单金额（元）
        model.setSubject(alipayBean.getSubject());
        model.setBody(alipayBean.getBody());
        model.setProductCode("QUICK_WAP_PAY");//产品代码，固定QUICK_WAP_PAY
        model.setTimeoutExpress(alipayBean.getExpireTime());//订单过期时间
        alipayRequest.setBizModel(model);

        alipayRequest.setReturnUrl(returnUrl);
        alipayRequest.setNotifyUrl(notifyUrl);


        //请求支付宝下单接口，发起http请求
        try {
            AlipayTradeWapPayResponse response = alipayClient.pageExecute(alipayRequest);
            PaymentResponseDTO paymentResponseDTO = new PaymentResponseDTO();
            log.info("调用支付宝下单接口，响应内容为：{}", response.getBody());
            paymentResponseDTO.setContent(response.getBody());
            return paymentResponseDTO;

        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new BusinessException(CommonErrorCode.E_400002);
        }





    }
}
