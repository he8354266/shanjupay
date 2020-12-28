package com.shanjupay.transaction.controller;

import com.alibaba.fastjson.JSON;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.util.AmountUtil;
import com.shanjupay.common.util.EncryptUtil;
import com.shanjupay.common.util.IPUtil;
import com.shanjupay.common.util.ParseURLPairUtil;
import com.shanjupay.merchant.api.AppService;
import com.shanjupay.merchant.api.dto.AppDTO;
import com.shanjupay.paymentagent.api.dto.PaymentResponseDTO;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.PayOrderDTO;
import com.shanjupay.transaction.convert.PayOrderConvert;
import com.shanjupay.transaction.vo.OrderConfirmVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Title: project
 * @Package * @Description:     * @author CodingSir
 * @date 2020/12/2411:47
 */
@Controller
@Slf4j
public class PayController {
    @Reference
    private TransactionService transactionService;
    @Reference
    private AppService appService;

    /**
     * 支付入口
     *
     * @param ticket  传入数据，对json数据进行的base64编码
     * @param request
     * @return
     */
    @RequestMapping("/pay-entry/{ticket}")
    public String payEntry(@PathVariable("ticket") String ticket, HttpServletRequest request) throws Exception {
        String jsonString = EncryptUtil.decodeUTF8StringBase64(ticket);
        PayOrderDTO payOrderDTO = JSON.parseObject(jsonString, PayOrderDTO.class);
        String params = ParseURLPairUtil.parseURLPair(payOrderDTO);
        BrowserType browserType = BrowserType.valueOfUserAgent(request.getHeader("User-Agent"));
        switch (browserType) {
            case ALIPAY:
                return "forward:/pay-page?" + params;
            case WECHAT:
                return "forward:/pay-page?" + params;
            default:
        }

        return "forward:/pay-page-error";
    }

    /**
     * 支付宝的下单接口,前端订单确认页面，点击确认支付，请求进来
     *
     * @param orderConfirmVO 订单信息
     * @param request
     * @param response
     */
    @ApiOperation("支付宝门店下单付款")
    @PostMapping("/createAliPayOrder")
    public void createAlipayOrderForStore(OrderConfirmVO orderConfirmVO, HttpServletRequest request, HttpServletResponse response) throws IOException {
        PayOrderDTO payOrderDTO = PayOrderConvert.INSTANCE.vo2dto(orderConfirmVO);
        //应用id
        String appId = payOrderDTO.getAppId();
        AppDTO appDTO = appService.getAppById(appId);
        payOrderDTO.setMerchantId(appDTO.getMerchantId());//商户id
        //将前端输入的元转成分
        try {
            payOrderDTO.setTotalAmount(Integer.valueOf(AmountUtil.changeF2Y(orderConfirmVO.getTotalAmount().toString())));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //客户端Ip
        payOrderDTO.setClientIp(IPUtil.getIpAddr(request));
        //保存订单
        PaymentResponseDTO<String> paymentResponseDTO = transactionService.submitOrderByAli(payOrderDTO);

        //支付宝下单接口响应
        String content = paymentResponseDTO.getContent();
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write(content);
        response.getWriter().flush();
        response.getWriter().close();
    }

}
