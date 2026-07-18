package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.entity.Order;
import com.example.fruitmallmanagementsystem.entity.Payment;
import com.example.fruitmallmanagementsystem.dto.PaymentCreateResult;
import com.example.fruitmallmanagementsystem.dto.PaymentCapabilities;
import com.example.fruitmallmanagementsystem.mapper.PaymentMapper;
import com.example.fruitmallmanagementsystem.service.OrderService;
import com.example.fruitmallmanagementsystem.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.List;

@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {
    private final OrderService orderService;
    private final boolean demoEnabled;

    private static final Set<String> CHANNELS = Set.of("DEMO", "WECHAT", "ALIPAY");
    private static final Map<String, String> CHANNEL_NAMES = Map.of(
            "DEMO", "演示支付",
            "WECHAT", "微信支付",
            "ALIPAY", "支付宝"
    );

    public PaymentServiceImpl(OrderService orderService,
                              @Value("${app.payment.demo-enabled:true}") boolean demoEnabled) {
        this.orderService = orderService;
        this.demoEnabled = demoEnabled;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment createDemoPayment(String orderNo, Long userId) {
        return createPayment(orderNo, userId, "DEMO").getPayment();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentCreateResult createPayment(String orderNo, Long userId, String requestedChannel) {
        if (!StringUtils.hasText(orderNo) || userId == null) {
            throw new IllegalArgumentException("订单号和用户不能为空");
        }
        String channel = StringUtils.hasText(requestedChannel)
                ? requestedChannel.trim().toUpperCase(Locale.ROOT) : "DEMO";
        if (!CHANNELS.contains(channel)) {
            throw new IllegalArgumentException("不支持的支付方式");
        }
        if (!demoEnabled) {
            throw new IllegalStateException("真实支付渠道尚未配置，已阻止创建无法完成的支付单");
        }
        Order order = orderService.getById(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new IllegalStateException("不能支付其他用户的订单");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new IllegalStateException("订单当前状态不可支付");
        }
        if (order.getExpireTime() != null && !order.getExpireTime().isAfter(LocalDateTime.now())) {
            orderService.changeUserOrderStatus(orderNo, userId, 4);
            throw new IllegalStateException("订单已超过支付时间并自动取消");
        }

        Payment paid = getOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo)
                .eq(Payment::getStatus, 1)
                .last("LIMIT 1"));
        if (paid != null) {
            return buildResult(paid);
        }

        Payment existing = getOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo)
                .eq(Payment::getChannel, channel)
                .eq(Payment::getStatus, 0)
                .orderByDesc(Payment::getCreateTime)
                .last("LIMIT 1"));
        if (existing != null) {
            return buildResult(existing);
        }

        update(new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo)
                .eq(Payment::getStatus, 0)
                .set(Payment::getStatus, 2));

        Payment payment = new Payment();
        payment.setPaymentNo("PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setChannel(channel);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(0);
        payment.setCreateTime(LocalDateTime.now());
        save(payment);
        return buildResult(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment confirmDemoPayment(String paymentNo, Long userId) {
        Payment payment = getOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) {
            throw new IllegalArgumentException("支付单不存在");
        }
        if (!userId.equals(payment.getUserId())) {
            throw new IllegalStateException("不能操作其他用户的支付单");
        }
        if (payment.getStatus() != null && payment.getStatus() == 1) {
            return payment;
        }
        if (payment.getStatus() == null || payment.getStatus() != 0) {
            throw new IllegalStateException("支付单当前状态不可支付");
        }
        if (!demoEnabled) {
            throw new IllegalStateException("当前渠道已切换为正式模式，必须由支付平台回调确认");
        }

        LocalDateTime paidAt = LocalDateTime.now();
        String tradeNo = "DEMO-" + payment.getChannel() + "-"
                + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        boolean updated = update(new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getId, payment.getId())
                .eq(Payment::getStatus, 0)
                .set(Payment::getStatus, 1)
                .set(Payment::getProviderTradeNo, tradeNo)
                .set(Payment::getPaidTime, paidAt));
        if (!updated) {
            Payment latest = getById(payment.getId());
            if (latest != null && latest.getStatus() != null && latest.getStatus() == 1) {
                return latest;
            }
            throw new IllegalStateException("支付状态已变化，请刷新后重试");
        }
        if (!orderService.markPaid(payment.getOrderNo(), paidAt)) {
            throw new IllegalStateException("订单状态已变化，支付确认已回滚");
        }
        return getById(payment.getId());
    }

    @Override
    public Page<Payment> pageList(Integer pageNum, Integer pageSize, String orderNo, Integer status) {
        long current = pageNum == null ? 1 : Math.max(1, pageNum);
        long size = pageSize == null ? 10 : Math.min(100, Math.max(1, pageSize));
        LambdaQueryWrapper<Payment> query = new LambdaQueryWrapper<>();
        query.like(StringUtils.hasText(orderNo), Payment::getOrderNo, orderNo)
                .eq(status != null && status >= 0, Payment::getStatus, status)
                .orderByDesc(Payment::getCreateTime);
        return page(new Page<>(current, size), query);
    }

    @Override
    public Payment getLatestForUser(String orderNo, Long userId) {
        return getOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo)
                .eq(Payment::getUserId, userId)
                .orderByDesc(Payment::getCreateTime)
                .last("LIMIT 1"));
    }

    @Override
    public PaymentCapabilities getCapabilities() {
        if (!demoEnabled) {
            return new PaymentCapabilities(false, List.of());
        }
        return new PaymentCapabilities(true, List.of(
                new PaymentCapabilities.Channel("WECHAT", "微信支付（演示）", "模拟扫码，不产生真实扣款", true),
                new PaymentCapabilities.Channel("ALIPAY", "支付宝（演示）", "模拟电脑支付，不产生真实扣款", true),
                new PaymentCapabilities.Channel("DEMO", "演示支付", "本地订单与库存闭环", true)
        ));
    }

    private PaymentCreateResult buildResult(Payment payment) {
        PaymentCreateResult result = new PaymentCreateResult();
        result.setPayment(payment);
        result.setChannelName(CHANNEL_NAMES.getOrDefault(payment.getChannel(), payment.getChannel()));
        result.setDemoMode(demoEnabled);
        result.setInstruction(result.isDemoMode()
                ? "当前为毕业设计演示环境，不会产生真实扣款"
                : "请在支付平台完成付款，支付结果以后端通知为准");
        return result;
    }
}
