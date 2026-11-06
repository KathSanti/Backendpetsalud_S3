package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.service.VentaService;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@AllArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    @PostMapping
    public ResponseEntity<Venta> realizarVenta(
            @RequestParam Long usuarioId,
            @RequestBody List<Long> productosIds) {
        Venta nuevaVenta = ventaService.realizarVenta(usuarioId, productosIds);
        return ResponseEntity.ok(nuevaVenta);
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.venta(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<Venta> obtenerVentaPorId(@PathVariable Long id) {
        return ventaService.obtenerVentaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.usuario(authentication, #usuarioId)")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Venta>> listarVentasPorUsuario(@PathVariable Long usuarioId) {
        List<Venta> ventas = ventaService.listarVentasPorUsuario(usuarioId);
        return ResponseEntity.ok(ventas);
    }

    @PreAuthorize("hasRole('GERENTE')")
    @GetMapping
    public ResponseEntity<List<Venta>> listarTodasLasVentas() {
        List<Venta> ventas = ventaService.listarTodasLasVentas();
        return ResponseEntity.ok(ventas);
    }
}
