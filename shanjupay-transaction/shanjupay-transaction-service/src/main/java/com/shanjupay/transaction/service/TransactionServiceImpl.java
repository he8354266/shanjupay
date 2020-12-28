package com.shanjupay.transaction.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.util.AmountUtil;
import com.shanjupay.common.util.EncryptUtil;
import com.shanjupay.common.util.PaymentUtil;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.paymentagent.api.PayChannelAgentService;
import com.shanjupay.paymentagent.api.conf.AliConfigParam;
import com.shanjupay.paymentagent.api.dto.AlipayBean;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
