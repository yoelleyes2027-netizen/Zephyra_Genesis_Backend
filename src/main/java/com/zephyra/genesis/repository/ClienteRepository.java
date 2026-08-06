package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {
    @Query("select c from ClienteEntity c where type(c) = ClienteComunEntity order by c.name")
    List<ClienteEntity> findAll();

    @Query("select c from ClienteEntity c where type(c) = ClienteComunEntity and lower(c.email) = lower(:email)")
    Optional<ClienteEntity> findByEmailIgnoreCase(String email);

    @Query("select c from ClienteEntity c where type(c) = ClienteComunEntity and lower(c.name) like lower(concat('%', :name, '%')) order by c.name asc")
    List<ClienteEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    @Query("select case when count(c) > 0 then true else false end from ClienteEntity c where type(c) = ClienteComunEntity and lower(c.email) = lower(:email)")
    boolean existsByEmailIgnoreCase(String email);
}