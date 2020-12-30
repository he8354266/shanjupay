package com.shanjupay.transaction.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.AmountUtil;
import com.shanjupay.common.util.EncryptUtil;
import com.shanjupay.common.util.PaymentUtil;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.conf.WXConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.paymentagent.api.dto.WeChatBean;
import com.shanjupay.transaction.api.PayChannelService;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.PayChannelParamDTO;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.api.dto.QRCodeDto;
import com.shanjupay.transaction.convert.PayOrderConvert;
import com.shanjupay.transaction.entity.PayOrder;
import com.shanjupay.transaction.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;


import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * @Title: project
 * @Package * @Description:     * @author CodingSir
 * @date 2020/12/249:13
 */
@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    @Value("${shanjupay.payurl}")
    private String payurl;
    @Value("${weixin.oauth2RequestUrl}")
    private String oauth2RequestUrl;
    @Value("${weixin.oauth2CodeReturnUrl}")
    private String oauth2CodeReturnUrl;
    @Value("${weixin.oauth2Token}")
    private String oauth2Token;
    @Reference
    private AppService appService;
    @Reference
    private MerchantService merchantService;
    @Reference
    private PayChannelAgentService payChannelAgentService;
    @Autowired
    private PayOrderMapper payOrderMapper;
    @Autowired
    private PayChannelService payChannelService;

    /**
     * 生成门店二维码的url
     *
     * @param qrCodeDto@return 支付入口（url），要携带参数（将传入的参数转成json，用base64编码）
     * @throws BusinessException
     */
    @Override
    public String createStoreQRCode(QRCodeDto qrCodeDto) throws BusinessException {
        //校验商户id和应用id和门店id的合法性
        verifyAppAndStore(qrCodeDto.getMerchantId(), qrCodeDto.getAppId(), qrCodeDto.getStoreId());
        //组装url所需要的数据
        PayOrderDTO payOrderDTO = new PayOrderDTO();
        payOrderDTO.setMerchantId(qrCodeDto.getMerchantId());
        payOrderDTO.setAppId(qrCodeDto.getAppId());
        payOrderDTO.setStoreId(qrCodeDto.getStoreId());
        payOrderDTO.setSubject(qrCodeDto.getSubject());//显示订单标题
        payOrderDTO.setChannel("shanju_c2b");//服务类型，要写为c扫b的服务类型
        payOrderDTO.setBody(qrCodeDto.getBody());//订单内容
        //转成json
        String jsonString = JSON.toJSONString(payOrderDTO);
        //base64编码
        String ticket = EncryptUtil.encodeUTF8StringBase64(jsonString);
        //目标是生成一个支付入口 的url，需要携带参数将传入的参数转成json，用base64编码
        String url = payurl + ticket;
        return url;
    }


    @Override
    public PaymentResponseDTO submitOrderByAli(PayOrderDTO payOrderDTO) {
        payOrderDTO.setChannel("ALIPAY_WAP");
        PayOrderDTO payOrderDTO1 = save(payOrderDTO);
        //调用支付渠道代理服务支付宝下单接口
        alipayH5(payOrderDTO1.getTradeNo());
        return null;
    }

    //调用支付渠道代理服务的支付宝下单接口
    private PaymentResponseDTO alipayH5(String tradeNo) {
        PayOrderDTO payOrderDTO = queryPayOrder(tradeNo);

        AlipayBean alipayBean = new AlipayBean();
        alipayBean.setOutTradeNo(payOrderDTO.getTradeNo());//订单号
        try {
            alipayBean.setTotalAmount(AmountUtil.changeF2Y(String.valueOf(payOrderDTO.getTotalAmount())));
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(CommonErrorCode.E_300006);
        }
        alipayBean.setSubject(payOrderDTO.getSubject());
        alipayBean.setBody(payOrderDTO.getBody());

        //支付渠道配置参数，从数据库查询
        PayChannelParamDTO payChannelParamDTO = payChannelService.queryParamByAppPlatformAndPayChannel(payOrderDTO.getAppId(), "shanju_c2b", "ALIPAY_WAP");
        String paramJson = payChannelParamDTO.getParam();
        //支付渠道参数
        AliConfigParam aliConfigParam = JSON.parseObject(paramJson, AliConfigParam.class);
        //字符编码
        aliConfigParam.setCharest("utf-8");
        PaymentResponseDTO paymentResponseDTO = payChannelAgentService.createPayOrderByAliWAP(aliConfigParam, alipayBean);
        return paymentResponseDTO;
    }

    @Override
    public PayOrderDTO queryPayOrder(String tradeNo) {
        PayOrder payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getTradeNo, tradeNo));
        return PayOrderConvert.INSTANCE.entity2dto(payOrder);
    }

    @Override
    public void updateOrderTradeNoAndTradeState(String tradeNo, String payChannelTradeNo, String state) throws BusinessException {
        LambdaUpdateWrapper<PayOrder> payOrderLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        payOrderLambdaUpdateWrapper.eq(PayOrder::getTradeNo, tradeNo);
        payOrderLambdaUpdateWrapper.set(PayOrder::getTradeState, state);
        payOrderLambdaUpdateWrapper.set(PayOrder::getPayChannelTradeNo, payChannelTradeNo);
        if (state != null && state.equals("2")) {
            payOrderLambdaUpdateWrapper.set(PayOrder::getPaySuccessTime, LocalDateTime.now());
        }
        payOrderMapper.update(null, payOrderLambdaUpdateWrapper);
    }

    @Override
    public String getWXOAuth2Code(PayOrderDTO payOrderDTO) {
        //闪聚平台的应用id
        String appId = payOrderDTO.getAppId();
        //获取微信支付渠道参数
        PayChannelParamDTO payChannelParamDTO = payChannelService.queryParamByAppPlatformAndPayChannel(payOrderDTO.getAppId(), "payOrderDTO", "ALIPAY_WAP");
        String paramJson = payChannelParamDTO.getParam();
        //微信支付渠道参数
        WXConfigParam wxConfigParam = JSON.parseObject(paramJson, WXConfigParam.class);

        String jsonString = JSON.toJSONString(payOrderDTO);
        String state = EncryptUtil.encodeUTF8StringBase64(jsonString);

        try {
            String url = String.format("%s?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=%s#wechat_redirect",
                    oauth2RequestUrl, wxConfigParam.getAppId(), oauth2CodeReturnUrl, state);
            return "redirect:" + url;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "forward:/pay-page-error";
    }

    @Override
    public String getWXOAuthOpenId(String code, String appId) {
        //获取微信支付渠道参数
        PayChannelParamDTO payChannelParamDTO = payChannelService.queryParamByAppPlatformAndPayChannel(appId, "shanju_c2b", "WX_JSAPI");
        String param = payChannelParamDTO.getParam();
        //微信支付渠道参数
        WXConfigParam wxConfigParam = JSON.parseObject(param, WXConfigParam.class);
        String url = String.format("%s?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                oauth2Token, wxConfigParam.getAppId(), wxConfigParam.getAppSecret(), code);
        //申请openid，请求url
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        //申请openid接口响应的内容，其中包括了openid
        String body = exchange.getBody();
        log.info("申请openid响应的内容:{}", body);
        String openid = JSON.parseObject(body).getString("openid");
        return openid;


    }

    @Override
    public Map<String, String> submitOrderByWechat(PayOrderDTO payOrderDTO) throws BusinessException {
        String openId = payOrderDTO.getOpenId();
        //支付渠道
        payOrderDTO.setChannel("WX_JSAPI");
        //保存订单到闪聚平台数据库
        PayOrderDTO save = save(payOrderDTO);
        //调用支付渠道代理服务，调用微信下单接口
        return weChatJsapi(openId, save.getTradeNo());
    }

    private Map<String, String> weChatJsapi(String openId, String tradeNo) {
        //查询订单
        PayOrderDTO payOrderDTO = queryPayOrder(tradeNo);
        WeChatBean weChatBean = new WeChatBean();
        weChatBean.setOpenId(openId);
        weChatBean.setOutTradeNo(payOrderDTO.getTradeNo());
        weChatBean.setTotalFee(payOrderDTO.getTotalAmount());
        weChatBean.setSpbillCreateIp(payOrderDTO.getClientIp());
        weChatBean.setBody(payOrderDTO.getBody());
        weChatBean.setNotifyUrl("none");
        String appId = payOrderDTO.getAppId();
        //支付渠道配置参数，从数据库查询
        PayChannelParamDTO payChannelParamDTO = payChannelService.queryParamByAppPlatformAndPayChannel(appId, "shanju_c2b", "WX_JSAPI");
        String paramJson = payChannelParamDTO.getParam();

        WXConfigParam wxConfigParam = JSON.parseObject(paramJson, WXConfigParam.class);
        Map<String, String> payOrderByWeChatJSAPI = payChannelAgentService.createPayOrderByWeChatJSAPI(wxConfigParam, weChatBean);
        return payOrderByWeChatJSAPI;
    }

    private PayOrderDTO save(PayOrderDTO payOrderDTO) {
        PayOrder payOrder = PayOrderConvert.INSTANCE.dto2entity(payOrderDTO);
        //订单号
        payOrder.setTradeNo(PaymentUtil.genUniquePayOrderNo());//雪花算法
        payOrder.setCreateTime(LocalDateTime.now());//创建时间
        payOrder.setExpireTime(LocalDateTime.now().plus(30, ChronoUnit.MINUTES));//过期时间是30分钟后
        payOrder.setCurrency("CNY");//人民币
        payOrder.setTradeState("0");//订单状态，0：订单生成
        payOrderMapper.insert(payOrder);
        return PayOrderConvert.INSTANCE.entity2dto(payOrder);

    }

    //私有，校验商户id和应用id和门店id的合法性
    private void verifyAppAndStore(Long merchantId, String appId, Long storeId) {
        //根据 应用id和商户id查询
        Boolean aBoolean = appService.queryAppInMerchant(appId, merchantId);
        if (!aBoolean) {
            throw new BusinessException(CommonErrorCode.E_200005);
        }
        Boolean aBoolean1 = merchantService.queryStoreInMerchant(storeId, merchantId);
        if (!aBoolean1) {
            throw new BusinessException(CommonErrorCode.E_200006);
        }
    }

}
