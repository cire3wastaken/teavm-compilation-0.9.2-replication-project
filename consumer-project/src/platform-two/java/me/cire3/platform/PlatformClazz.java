package me.cire3.platform;

import com.google.common.base.Objects;

public class PlatformClazz {
    public static void foo(Object o) {
        System.out.println(Objects.toStringHelper(o).add("aValue", 2));
    }
}
