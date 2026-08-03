-- =============================================================================
-- 28_delivery_menu_route_name.sql
-- 修复「投递记录」菜单 route_name 缺失。
-- route_name 为空时后端兜底取 path 首字母大写（Delivery），与前端组件名
-- ReviewDeliveryRecord 不一致，keep-alive 无法缓存该页；页面依赖 onActivated
-- 加载数据，导致列表永远转圈。补齐后与其余列表页约定一致。
-- =============================================================================

UPDATE sys_menu
SET route_name = 'ReviewDeliveryRecord',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE menu_id = 130;
