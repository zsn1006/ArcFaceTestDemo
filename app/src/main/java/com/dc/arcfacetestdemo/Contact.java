package com.dc.arcfacetestdemo;

import java.io.Serializable;

/**
 * Created by zsn10 on 2017/10/17.
 */

public class Contact implements Serializable{
    private String name;
    private String pic;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }
}
