package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.TicketEntity;
import com.zephyra.genesis.entity.FORMA_DE_PAGO;
import com.zephyra.genesis.entity.TIPO_MONEDA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {
	boolean existsByCliente_Id(Long clienteId);

	@Query("""
			SELECT COALESCE(SUM(t.montoTotal), 0)
			FROM TicketEntity t
			WHERE t.usuario.id = :usuarioId
			  AND t.fechaCreacion >= :fechaInicio
			  AND t.formaDePago = :formaDePago
			  AND t.egreso = false
			  AND t.devolucion = false
			""")
	Float sumarMontoPorUsuarioDesdeYFormaDePago(
			@Param("usuarioId") Long usuarioId,
			@Param("fechaInicio") Date fechaInicio,
			@Param("formaDePago") FORMA_DE_PAGO formaDePago);

	@Query("""
			SELECT COALESCE(SUM(t.montoTotal), 0)
			FROM TicketEntity t
			WHERE t.usuario.id = :usuarioId
			  AND t.fechaCreacion >= :fechaInicio
			  AND t.egreso = false
			""")
	Float sumarIngresosPorUsuarioDesde(
			@Param("usuarioId") Long usuarioId,
			@Param("fechaInicio") Date fechaInicio);

	@Query("""
			SELECT COALESCE(SUM(t.montoTotal), 0)
			FROM TicketEntity t
			WHERE t.usuario.id = :usuarioId
			  AND t.fechaCreacion >= :fechaInicio
			  AND t.egreso = true
			""")
	Float sumarEgresosPorUsuarioDesde(
			@Param("usuarioId") Long usuarioId,
			@Param("fechaInicio") Date fechaInicio);

	@Query("""
			SELECT COALESCE(SUM(t.montoPagado), 0)
			FROM TicketEntity t
			WHERE t.usuario.id = :usuarioId
			  AND t.fechaCreacion >= :fechaInicio
			  AND t.formaDePago = :formaDePago
			  AND t.tipoMoneda = :tipoMoneda
			  AND t.egreso = false
			  AND t.devolucion = false
			""")
	Float sumarMontoPagadoPorUsuarioDesdeYFormaDePagoYMoneda(
			@Param("usuarioId") Long usuarioId,
			@Param("fechaInicio") Date fechaInicio,
			@Param("formaDePago") FORMA_DE_PAGO formaDePago,
			@Param("tipoMoneda") TIPO_MONEDA tipoMoneda);
}