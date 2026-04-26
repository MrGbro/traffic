package io.homeey.traffic.routing.matcher;

public class PathMatcher {

    public boolean matches(String actualPath, String expectedPath) {
        if (actualPath == null || expectedPath == null) {
            return false;
        }
        return actualPath.equals(expectedPath);
    }
}
