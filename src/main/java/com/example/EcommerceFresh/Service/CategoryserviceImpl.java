package com.example.EcommerceFresh.Service;

import com.example.EcommerceFresh.Dao.CatDao;
import com.example.EcommerceFresh.Entity.Category;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryserviceImpl {
    private CatDao catDao;
    public CategoryserviceImpl(CatDao catDao){
        this.catDao=catDao;
    }

    public List<Category> getAllCategory(){
        return catDao.findAll();
    }

    public void Save(Category cat){
        catDao.save(cat);
    }

//    public Integer getIdByName(String name){
//        Category tempCategory= catDao.findByName(name);
//        if(tempCategory!=null) {
//            return tempCategory.getId();
//        }
//        return null;
//    }

    public void deleteCat(int id){
        catDao.deleteById(id);
    }

    public Optional<Category> findCategoryById(int id){
        Optional<Category> temp=catDao.findById(id);
        return temp;
    }
}
