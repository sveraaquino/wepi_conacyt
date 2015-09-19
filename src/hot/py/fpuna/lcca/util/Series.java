package py.fpuna.lcca.util;

import java.util.ArrayList;
import java.util.List;

public class Series {
    private String name;
    private List<Long> data;
 
    public Series() {}
 
    public Series(String name, int [] data) {
        this.name = name;
        
        this.data = new ArrayList<Long>();	
        for (int i = 0;i<data.length;i++){
        	this.data.add((long)data[i]);
        }
        	
    }
}