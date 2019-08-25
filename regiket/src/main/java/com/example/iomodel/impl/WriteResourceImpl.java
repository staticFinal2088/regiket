package com.example.iomodel.impl;

import com.example.iomodel.SnowflakeIdWorker;
import com.example.iomodel.WriteResource;

public class WriteResourceImpl implements WriteResource{
	@Override
	public Long WriteResource() {
		for(int i = 0 ; i < 32 ; i++){
    		for(int j = 0 ; j < 32 ; j ++){
    			SnowflakeIdWorker snow = new SnowflakeIdWorker(i,j);
    			System.out.println(snow.nextId());
    		}
    	}
		return null;
	}
}
