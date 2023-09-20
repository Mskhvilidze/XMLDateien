package com.company;

public enum Tags {
    ASSETS("Assets"),
    PRODUCTS("Products");
    String key;

    Tags(String str) {
        key = str;
    }

    public String value() {
        return this.key;
    }
}
