package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.dto.OrderCreateResult;
import com.example.fruitmallmanagementsystem.dto.OrderDto;
import com.example.fruitmallmanagementsystem.entity.Order;
import com.example.fruitmallmanagementsystem.entity.OrderItem;
import com.example.fruitmallmanagementsystem.entity.OrderLog;
import com.example.fruitmallmanagementsystem.entity.Product;
import com.example.fruitmallmanagementsystem.mapper.OrderItemMapper;
import com.example.fruitmallmanagementsystem.mapper.OrderLogMapper;
import com.example.fruitmallmanagementsystem.mapper.OrderMapper;
import com.example.fruitmallmanagementsystem.mapper.ProductMapper;
import com.example.fruitmallmanagementsystem.mapper.InventoryMovementMapper;
import com.example.fruitmallmanagementsystem.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final InventoryMovementMapper inventoryMovementMapper;
    private final long paymentTimeoutMinutes;

    public OrderServiceImpl(ProductMapper productMapper, OrderItemMapper orderItemMapper,
                            OrderLogMapper orderLogMapper, InventoryMovementMapper inventoryMovementMapper,
                            @Value("${app.order.payment-timeout-minutes:30}") long paymentTimeoutMinutes) {
        this.productMapper = productMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderLogMapper = orderLogMapper;
        this.inventoryMovementMapper = inventoryMovementMapper;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResult createOrder(OrderDto dto, Long userId) {
        validateOrder(dto, userId);
        String requestId = normalizeRequestId(dto.getRequestId());
        Order existing = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRequestId, requestId)
                .last("LIMIT 1"));
        if (existing != null) return result(existing);

        String orderNo = "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> productIds = new HashSet<>();

        for (OrderItem item : dto.getItems()) {
            if (!productIds.add(item.getProductId())) throw new IllegalArgumentException("同一商品请合并数量后再提交");
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) throw new IllegalArgumentException("商品不存在：" + item.getProductId());
            if (!Integer.valueOf(1).equals(product.getStatus())) throw new IllegalStateException("商品已下架：" + product.getName());
            if (product.getPrice() == null || product.getPrice().signum() < 0) throw new IllegalStateException("商品价格异常：" + product.getName());
            if (product.getStock() == null || product.getStock() < item.getCount()) throw new IllegalStateException("库存不足：" + product.getName());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getCount())));
            item.setOrderNo(orderNo);
            item.setProductName(product.getName());
            item.setPrice(product.getPrice());
            item.setProductImage(product.getImage());
            item.setProductUnit(product.getUnit());
        }
        total = total.setScale(2, java.math.RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setRequestId(requestId);
        order.setCustomerName(dto.getCustomerName().trim());
        order.setCustomerPhone(dto.getCustomerPhone().trim());
        order.setAddress(dto.getAddress().trim());
        order.setTotalAmount(total);
        order.setStatus(0);
        order.setArchived(0);
        order.setCreateTime(now);
        order.setExpireTime(now.plusMinutes(paymentTimeoutMinutes));
        baseMapper.insert(order);

        for (OrderItem item : dto.getItems()) {
            if (productMapper.deductStock(item.getProductId(), item.getCount()) != 1) {
                throw new IllegalStateException("库存刚刚发生变化，请刷新购物车后重试：" + item.getProductName());
            }
            inventoryMovementMapper.record(item.getProductId(), -item.getCount(), "ORDER_DEDUCT",
                    orderNo, userId, "提交订单扣减库存");
            item.setId(null);
            orderItemMapper.insert(item);
        }
        addOrderLog(orderNo, null, 0, "CUSTOMER", userId, requestId, "用户提交订单，等待支付");
        return result(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(String orderNo, LocalDateTime paymentTime) {
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo).eq(Order::getStatus, 0)
                .set(Order::getStatus, 1).set(Order::getPaymentTime, paymentTime));
        if (updated) {
            orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo))
                    .forEach(item -> productMapper.addSales(item.getProductId(), item.getCount()));
            addOrderLog(orderNo, 0, 1, "PAYMENT", null, null, "支付成功，等待商家发货");
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(String orderNo, Integer targetStatus) {
        transition(orderNo, targetStatus, "ADMIN", null);
    }

    private void transition(String orderNo, Integer targetStatus, String source, Long operatorId) {
        Order order = getById(orderNo);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        int current = order.getStatus() == null ? -1 : order.getStatus();
        boolean valid = (current == 1 && targetStatus == 2)
                || (current == 2 && targetStatus == 3)
                || (current == 0 && targetStatus == 4);
        if (!valid) throw new IllegalStateException("非法订单状态流转；支付必须通过支付接口完成");
        LambdaUpdateWrapper<Order> update = new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo).eq(Order::getStatus, current).set(Order::getStatus, targetStatus);
        if (targetStatus == 2) update.set(Order::getDeliveryTime, LocalDateTime.now());
        if (targetStatus == 3) update.set(Order::getReceiveTime, LocalDateTime.now());
        if (targetStatus == 4) update.set(Order::getCancelTime, LocalDateTime.now());
        if (!this.update(update)) throw new IllegalStateException("订单状态已变化，请刷新后重试");
        if (targetStatus == 4) restoreStock(orderNo);
        addOrderLog(orderNo, current, targetStatus, source, operatorId, null, statusRemark(targetStatus, source));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCancelledOrder(String orderNo) {
        Order order = getById(orderNo);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        if (!Integer.valueOf(4).equals(order.getStatus())) throw new IllegalStateException("只有已取消订单才能归档");
        update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo).eq(Order::getStatus, 4).set(Order::getArchived, 1));
        addOrderLog(orderNo, 4, 4, "ADMIN", null, null, "订单已归档，业务数据继续保留");
    }

    @Override
    public OrderDto getOrderWithItems(String orderNo) {
        Order order = getById(orderNo);
        if (order == null) return null;
        OrderDto dto = new OrderDto();
        BeanUtils.copyProperties(order, dto);
        dto.setItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, orderNo).orderByAsc(OrderItem::getId)));
        return dto;
    }

    @Override
    public OrderDto getOrderWithItemsForUser(String orderNo, Long userId) {
        OrderDto dto = getOrderWithItems(orderNo);
        return dto != null && userId != null && userId.equals(dto.getUserId()) ? dto : null;
    }

    @Override
    public Page<Order> pageMyOrders(Long userId, Integer pageNum, Integer pageSize, String keyword, Integer status) {
        long current = pageNum == null ? 1 : Math.max(1, pageNum);
        long size = pageSize == null ? 10 : Math.min(50, Math.max(1, pageSize));
        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null && status >= 0, Order::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper.like(Order::getOrderNo, value).or()
                    .apply("EXISTS (SELECT 1 FROM sys_order_item oi WHERE oi.order_no = sys_order.order_no " +
                            "AND oi.product_name LIKE CONCAT('%', {0}, '%'))", value));
        }
        query.orderByDesc(Order::getCreateTime);
        return page(new Page<>(current, size), query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeUserOrderStatus(String orderNo, Long userId, Integer targetStatus) {
        Order order = getById(orderNo);
        if (order == null || userId == null || !userId.equals(order.getUserId())) throw new IllegalArgumentException("订单不存在");
        if (targetStatus == 4 && !Integer.valueOf(0).equals(order.getStatus())) throw new IllegalStateException("只有待支付订单可以取消");
        if (targetStatus == 3 && !Integer.valueOf(2).equals(order.getStatus())) throw new IllegalStateException("只有待收货订单可以确认收货");
        if (targetStatus != 3 && targetStatus != 4) throw new IllegalArgumentException("不支持的用户订单操作");
        transition(orderNo, targetStatus, "CUSTOMER", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelExpiredOrders() {
        List<Order> expired = list(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0).isNotNull(Order::getExpireTime)
                .le(Order::getExpireTime, LocalDateTime.now()).last("LIMIT 100"));
        int count = 0;
        for (Order order : expired) {
            try {
                transition(order.getOrderNo(), 4, "SYSTEM", null);
                count++;
            } catch (IllegalStateException concurrentChange) {
                log.debug("Skip expired order {} because its status changed concurrently", order.getOrderNo());
            }
        }
        return count;
    }

    private void restoreStock(String orderNo) {
        for (OrderItem item : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, orderNo))) {
            productMapper.restoreStock(item.getProductId(), item.getCount());
            inventoryMovementMapper.record(item.getProductId(), item.getCount(), "ORDER_RESTORE",
                    orderNo, null, "订单取消恢复库存");
        }
    }

    private void validateOrder(OrderDto dto, Long userId) {
        if (dto == null || userId == null) throw new IllegalArgumentException("订单参数不完整");
        if (dto.getItems() == null || dto.getItems().isEmpty()) throw new IllegalArgumentException("订单至少包含一件商品");
        if (dto.getItems().size() > 50) throw new IllegalArgumentException("单个订单最多包含50种商品");
        if (!StringUtils.hasText(dto.getCustomerName()) || !StringUtils.hasText(dto.getCustomerPhone())
                || !StringUtils.hasText(dto.getAddress())) throw new IllegalArgumentException("收货人、联系电话和地址不能为空");
        if (dto.getCustomerName().length() > 50 || dto.getCustomerPhone().length() > 20 || dto.getAddress().length() > 255) {
            throw new IllegalArgumentException("收货信息长度超出限制");
        }
        for (OrderItem item : dto.getItems()) {
            if (item == null || item.getProductId() == null || item.getCount() == null
                    || item.getCount() <= 0 || item.getCount() > 999) {
                throw new IllegalArgumentException("商品ID和1-999之间的购买数量必须有效");
            }
        }
    }

    private String normalizeRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) return UUID.randomUUID().toString();
        String value = requestId.trim();
        if (value.length() > 64 || !value.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("请求标识格式不正确");
        return value;
    }

    private OrderCreateResult result(Order order) {
        return new OrderCreateResult(order.getOrderNo(), order.getTotalAmount(), order.getExpireTime());
    }

    private void addOrderLog(String orderNo, Integer before, Integer after, String source,
                             Long operatorId, String requestId, String remark) {
        OrderLog log = new OrderLog();
        log.setOrderNo(orderNo);
        log.setStatusBefore(before == null ? "新建" : statusText(before));
        log.setStatusAfter(statusText(after));
        log.setSource(source);
        log.setOperatorId(operatorId);
        log.setRequestId(requestId);
        log.setRemark(remark);
        log.setChangeTime(LocalDateTime.now());
        orderLogMapper.insert(log);
    }

    private String statusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "待支付"; case 1 -> "待发货"; case 2 -> "待收货";
            case 3 -> "已完成"; case 4 -> "已取消"; default -> "未知";
        };
    }

    private String statusRemark(Integer status, String source) {
        return switch (status) {
            case 2 -> "商家已发货，等待用户收货";
            case 3 -> "订单已确认收货并完成";
            case 4 -> "SYSTEM".equals(source) ? "订单支付超时，已自动取消并恢复库存" : "订单已取消并恢复库存";
            default -> "订单状态已更新";
        };
    }
}
