package org.linlinjava.litemall.admin.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class PermissionUtil {

    public static List<PermVo> listPermissions(ApplicationContext context, String basicPackage) {
        List<PermVo> root = new ArrayList<>();
        List<Permission> permissions = findPermissions(context, basicPackage);
        for(Permission permission : permissions) {
            RequiresPermissions requiresPermissions = permission.getRequiresPermissions();
            RequiresPermissionsDesc requiresPermissionsDesc = permission.getRequiresPermissionsDesc();
            String api = permission.getApi();

            String[] menus = requiresPermissionsDesc.menu();
            if(menus.length != 2){
                throw new RuntimeException("目前只支持两级菜单");
            }
            String menu1 = menus[0];
            PermVo perm1 = null;
            for(PermVo permVo : root){
                if(permVo.getLabel().equals(menu1)){
                    perm1 = permVo;
                    break;
                }
            }
            if(perm1 == null){
                perm1 = new PermVo();
                perm1.setId(menu1);
                perm1.setLabel(menu1);
                perm1.setChildren(new ArrayList<>());
                root.add(perm1);
            }
            String menu2 = menus[1];
            PermVo perm2 = null;
            for(PermVo permVo : perm1.getChildren()){
                if(permVo.getLabel().equals(menu2)){
                    perm2 = permVo;
                    break;
                }
            }
            if(perm2 == null){
                perm2 = new PermVo();
                perm2.setId(menu2);
                perm2.setLabel(menu2);
                perm2.setChildren(new ArrayList<>());
                perm1.getChildren().add(perm2);
            }

            PermVo leftPerm = new PermVo();
            leftPerm.setId(requiresPermissions.value()[0]);
            leftPerm.setLabel(requiresPermissionsDesc.button());
            leftPerm.setApi(api);

            perm2.getChildren().add(leftPerm);
        }
        return root;
    }

    public static List<Permission> findPermissions(ApplicationContext context, String basicPackage) {
        Map<String, Object> map = context.getBeansWithAnnotation(Controller.class);
        List<Permission> permissions = new ArrayList<>();
        for(Map.Entry<String, Object> entry : map.entrySet()){
            Object bean = entry.getValue();
            if(!StringUtils.contains(ClassUtils.getPackageName(bean.getClass()), basicPackage)){
                continue;
            }

            Class<?> clz = bean.getClass();
            Class controllerClz = clz.getSuperclass();
            RequestMapping clazzRequestMapping = AnnotationUtils.findAnnotation(controllerClz, RequestMapping.class);
            List<Method> methods = MethodUtils.getMethodsListWithAnnotation(controllerClz, RequiresPermissions.class);
            for(Method method : methods){
                RequiresPermissions requiresPermissions = AnnotationUtils.getAnnotation(method, RequiresPermissions.class);
                RequiresPermissionsDesc requiresPermissionsDesc = AnnotationUtils.getAnnotation(method, RequiresPermissionsDesc.class);

                if(requiresPermissions == null || requiresPermissionsDesc == null){
                    continue;
                }

                String api = "";
                if(clazzRequestMapping != null){
                    api = clazzRequestMapping.value()[0];
                }

                PostMapping postMapping = AnnotationUtils.getAnnotation(method, PostMapping.class);
                if(postMapping != null){
                    api = "POST " + api + postMapping.value()[0];

                    Permission permission = new Permission();
                    permission.setRequiresPermissions(requiresPermissions);
                    permission.setRequiresPermissionsDesc(requiresPermissionsDesc);
                    permission.setApi(api);
                    permissions.add(permission);
                    continue;
                }
                GetMapping getMapping = AnnotationUtils.getAnnotation(method, GetMapping.class);
                if(getMapping != null){
                    api = "GET " + api + getMapping.value()[0];
                    Permission permission = new Permission();
                    permission.setRequiresPermissions(requiresPermissions);
                    permission.setRequiresPermissionsDesc(requiresPermissionsDesc);
                    permission.setApi(api);
                    permissions.add(permission);
                    continue;
                }
                // TODO
                // 这里只支持GetMapping注解或者PostMapping注解，应该进一步提供灵活性
                throw new RuntimeException("目前权限管理应该在method的前面使用GetMapping注解或者PostMapping注解");
            }
        }
        return permissions;
    }

}
