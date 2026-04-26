package io.homeey.traffic.routing.matcher;

public class MethodMatcher {

    public boolean matches(String actualMethod, String expectedMethod) {
        if (actualMethod == null || expectedMethod == null) {
            return false;
        }
        return actualMethod.equalsIgnoreCase(expectedMethod);
    }
}
