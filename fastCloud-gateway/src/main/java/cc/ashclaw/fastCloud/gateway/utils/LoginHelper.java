package cc.ashclaw.fastCloud.gateway.utils;

import cc.ashclaw.common4j.core.convert.Convert;
import cc.ashclaw.common4j.core.lang.ObjectUtil;
import cc.ashclaw.fastCloud.api.system.constant.TenantConstants;
import cc.ashclaw.fastCloud.api.system.constant.UserConstants;
import cc.ashclaw.fastCloud.api.system.enums.UserType;
import cc.ashclaw.fastCloud.api.system.model.LoginUser;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.Set;

/**
 * 登录助手工具类
 *
 * <p>该工具类封装了Sa-Token框架的登录相关操作，提供便捷的方法获取当前登录用户信息。
 * 主要功能包括：
 * <ul>
 *   <li>用户登录信息管理：登录、获取登录用户</li>
 *   <li>用户ID获取：当前用户ID、用户类型</li>
 *   <li>租户信息获取：租户ID、租户管理员判断</li>
 *   <li>部门信息获取：部门ID、部门名称</li>
 *   <li>权限判断：超级管理员判断、租户管理员判断</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * // 获取当前登录用户
 * LoginUser user = LoginHelper.getLoginUser();
 *
 * // 获取当前用户ID
 * Long userId = LoginHelper.getUserId();
 *
 * // 判断是否是超级管理员
 * boolean isSuperAdmin = LoginHelper.isSuperAdmin();
 *
 * // 判断是否已登录
 * boolean isLogin = LoginHelper.isLogin();
 * </pre>
 *
 * <p>Session存储结构：
 * <pre>
 * SaSession
 * ├── loginDevice: 登录设备类型
 * └── loginUser: LoginUser对象
 * </pre>
 *
 * @author ashclaw
 * @since JDK 25
 * @see LoginUser
 * @see SaSession
 * @see StpUtil
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginHelper {

    /** Session key: 登录用户对象 */
    public static final String LOGIN_USER_KEY = "loginUser";

    /** Session key: 租户ID */
    public static final String TENANT_KEY = "tenantId";

    /** Session key: 用户ID */
    public static final String USER_KEY = "userId";

    /** Session key: 用户名 */
    public static final String USER_NAME_KEY = "userName";

    /** Session key: 部门ID */
    public static final String DEPT_KEY = "deptId";

    /** Session key: 部门名称 */
    public static final String DEPT_NAME_KEY = "deptName";

    /** Session key: 部门类型 */
    public static final String DEPT_CATEGORY_KEY = "deptCategory";

    /** Session key: 客户端ID（用于设备绑定） */
    public static final String CLIENT_KEY = "clientId";

    /**
     * 用户登录
     *
     * <p>将用户登录信息存储到Sa-Token的Session中。
     *
     * @param loginUser 登录用户信息
     * @param device 登录设备类型
     * @param timeout 会话超时时间（秒）
     */
    public static void login(LoginUser loginUser, String device, long timeout) {
        StpUtil.login(loginUser.getLoginId());
        StpUtil.getTokenSession().set("loginDevice", device);
        StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * 获取当前登录用户
     *
     * <p>从当前会话中获取登录用户信息。
     *
     * @return 登录用户对象，如果未登录则返回null
     */
    public static LoginUser getLoginUser() {
        SaSession session = StpUtil.getTokenSession();
        if (ObjectUtil.isNull(session)) {
            return null;
        }
        return (LoginUser) session.get(LOGIN_USER_KEY);
    }

    /**
     * 根据Token获取登录用户
     *
     * <p>根据指定的Token获取对应的登录用户信息。
     *
     * @param token 用户Token
     * @return 登录用户对象，如果Token无效则返回null
     */
    public static LoginUser getLoginUser(String token) {
        SaSession session = StpUtil.getTokenSessionByToken(token);
        if (ObjectUtil.isNull(session)) {
            return null;
        }
        return (LoginUser) session.get(LOGIN_USER_KEY);
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果未登录则返回null
     */
    public static Long getUserId() {
        return Convert.toLong(getExtra(USER_KEY));
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果未登录则返回null
     */
    public static String getUserName() {
        return Convert.toStr(getExtra(USER_NAME_KEY));
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，如果未登录则返回null
     */
    public static String getTenantId() {
        return Convert.toStr(getExtra(TENANT_KEY));
    }

    /**
     * 获取当前部门ID
     *
     * @return 部门ID，如果未登录则返回null
     */
    public static Long getDeptId() {
        return Convert.toLong(getExtra(DEPT_KEY));
    }

    /**
     * 获取当前部门名称
     *
     * @return 部门名称，如果未登录则返回null
     */
    public static String getDeptName() {
        return Convert.toStr(getExtra(DEPT_NAME_KEY));
    }

    /**
     * 获取当前部门类型
     *
     * @return 部门类型，如果未登录则返回null
     */
    public static String getDeptCategory() {
        return Convert.toStr(getExtra(DEPT_CATEGORY_KEY));
    }

    /**
     * 获取Token中的扩展信息
     *
     * @param key 扩展信息key
     * @return 扩展信息值，如果不存在或未登录则返回null
     */
    private static Object getExtra(String key) {
        try {
            return StpUtil.getExtra(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前用户类型
     *
     * @return 用户类型枚举
     */
    public static UserType getUserType() {
        String loginType = StpUtil.getLoginIdAsString();
        return UserType.getUserType(loginType);
    }

    /**
     * 判断当前用户是否是超级管理员
     *
     * @return true表示是超级管理员，false表示不是
     */
    public static boolean isSuperAdmin() {
        return isSuperAdmin(getUserId());
    }

    /**
     * 判断指定用户是否是超级管理员
     *
     * @param userId 用户ID
     * @return true表示是超级管理员，false表示不是
     */
    public static boolean isSuperAdmin(Long userId) {
        return UserConstants.SUPER_ADMIN_ID.equals(userId);
    }

    /**
     * 判断当前用户是否是租户管理员
     *
     * @return true表示是租户管理员，false表示不是
     */
    public static boolean isTenantAdmin() {
        return Convert.toBoolean(isTenantAdmin(Objects.requireNonNull(getLoginUser()).getRolePermission()));
    }

    /**
     * 根据角色权限判断是否是租户管理员
     *
     * @param rolePermission 角色权限集合
     * @return true表示是租户管理员，false表示不是
     */
    public static boolean isTenantAdmin(Set<String> rolePermission) {
        if (CollectionUtils.isEmpty(rolePermission)) {
            return false;
        }
        return rolePermission.contains(TenantConstants.TENANT_ADMIN_ROLE_KEY);
    }

    /**
     * 判断当前用户是否已登录
     *
     * @return true表示已登录，false表示未登录
     */
    public static boolean isLogin() {
        try {
            return getLoginUser() != null;
        } catch (Exception e) {
            return false;
        }
    }
}