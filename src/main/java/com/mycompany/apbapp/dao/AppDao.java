/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apbapp.dao;

import java.util.List;
import java.util.Map;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

public interface AppDao<T> {
    
    public List<T> findAll();
    
    public List<T> findAll(int first, int pageSize, Map<String,SortMeta> sortBy, Map<String, FilterMeta> filterBy);
    
    public Long count(Map<String, FilterMeta> filterBy);
    
    public boolean executeSQL(String hql);
    
}
