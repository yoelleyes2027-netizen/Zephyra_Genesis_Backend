package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.CajaDiariaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CajaDiariaRepository extends JpaRepository<CajaDiariaEntity, Long> {
    Optional<CajaDiariaEntity> findByUsuario_Id(Long usuarioId);

    boolean existsByCajaGlobal_IdAndFechaInicioIsNotNullAndFechaCierreIsNull(Long cajaGlobalId);

    @Query(value = "SELECT " +
            "COALESCE(SUM(total_ingresos), 0), " +
            "COALESCE(SUM(total_egresos), 0), " +
            "COALESCE(SUM(transferencia_calculada), 0), " +
            "COALESCE(SUM(pos_calculado), 0), " +
            "COALESCE(SUM(pos_declarado), 0), " +
            "COALESCE(SUM(diferencia_pos), 0), " +
            "COALESCE(SUM(diferencia_efectivo), 0), " +
            "COALESCE(SUM(diferencia_dolares), 0), " +
            "COALESCE(SUM(efectivo_calculado), 0), " +
            "COALESCE(SUM(efectivo_declarado), 0), " +
            "COALESCE(SUM(dolares_declarados), 0), " +
            "COALESCE(SUM(dolares_calculados), 0) " +
            "FROM caja_diaria", nativeQuery = true)
        List<Object[]> obtenerTotalesCierreDia();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "TRUNCATE TABLE caja_diaria RESTRICT", nativeQuery = true)
    void truncateCajaDiaria();
}