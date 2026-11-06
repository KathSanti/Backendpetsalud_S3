package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.service.ProductoService;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@AllArgsConstructor 
public class ProductoController {

    private final ProductoService productoService;

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    @PostMapping
    public ResponseEntity<Producto> agregarProducto(@RequestBody Producto producto) {
        Producto nuevoProducto = productoService.agregarProducto(producto);
        return ResponseEntity.ok(nuevoProducto);
    }
    @PreAuthorize("hasAnyRole('CLIENTE','EMPLEADO','GERENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProductoPorId(@PathVariable Long id) {
        return productoService.obtenerProductoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable Long id, @RequestBody Producto producto) {
        Producto productoActualizado = productoService.actualizarProducto(id, producto);
        return ResponseEntity.ok(productoActualizado);
    }

    @PreAuthorize("hasRole('GERENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('CLIENTE','EMPLEADO','GERENTE')")
    @GetMapping
    public ResponseEntity<List<Producto>> listarProductos() {
        List<Producto> productos = productoService.listarProductos();
        return ResponseEntity.ok(productos);
    }
    @PreAuthorize("hasAnyRole('CLIENTE','EMPLEADO','GERENTE')")
    @GetMapping("/categoria")
    public ResponseEntity<List<Producto>> listarProductosPorCategoria(@RequestParam String categoria) {
        List<Producto> productos = productoService.listarProductosPorCategoria(categoria);
        return ResponseEntity.ok(productos);
    }
    
    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<Producto>> listarProductosConStockBajo(@RequestParam Integer cantidad) {
        List<Producto> productos = productoService.listarProductosConStockBajo(cantidad);
        return ResponseEntity.ok(productos);
    }
}
