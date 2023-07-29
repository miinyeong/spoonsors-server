package com.spoonsors.spoonsorsserver.repository.manager;

import com.spoonsors.spoonsorsserver.entity.Ingredients;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ManagerRepository {

    private final EntityManager em;

    //식재료 등록, 수정
    public void regist(Ingredients ingredient){
        em.persist(ingredient);
    }

    //모든 식재료 조회
    public List<Ingredients> findAll() {
        return em.createQuery("SELECT u FROM Ingredients u", Ingredients.class)
                .getResultList();
    }

    // 식재료 삭제
    public void remove(Long ingredients_id) {
        em.remove(findById(ingredients_id));
    }

    // 식재료 id값으로 조회
    public Ingredients findById(Long ingredients_id) {
        return em.find(Ingredients.class, ingredients_id);
    }

    // 식재료 이름으로 조회
    public Ingredients findByName(String findName){
        return em.createQuery("SELECT u FROM Ingredients u WHERE u.ingredients_name = :findName", Ingredients.class)
                .setParameter("findName", findName)
                .getSingleResult();
    }
}
