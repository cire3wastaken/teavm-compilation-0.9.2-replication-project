package me.cire3;

import com.google.common.base.Objects;
import me.cire3.platform.PlatformClazz;

public class EntryPoint {
    public static void main(String[] args) {
        PlatformClazz.foo(Objects.firstNonNull(null, new Object()));
    }
}
