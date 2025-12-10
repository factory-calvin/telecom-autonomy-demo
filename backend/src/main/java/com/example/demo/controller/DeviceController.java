package com.example.demo.controller;

import com.example.demo.model.Device;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.DeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceRepository repository;
    private final CustomerRepository customerRepository;

    public DeviceController(DeviceRepository repository, CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<DeviceResponse> getAllDevices() {
        return repository.findAll().stream().map(DeviceResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable Long id) {
        return repository.findById(id)
            .map(device -> ResponseEntity.ok(DeviceResponse.from(device)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public DeviceResponse createDevice(@RequestBody DeviceRequest request) {
        Device device = new Device(request.imei(), request.model(), request.simNumber());
        if (request.customerId() != null) {
            customerRepository.findById(request.customerId()).ifPresent(customer -> {
                device.setCustomer(customer);
                device.setStatus(Device.Status.ASSIGNED);
                device.setAssignedAt(Instant.now());
            });
        }
        return DeviceResponse.from(repository.save(device));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id, @RequestBody DeviceRequest request) {
        return repository.findById(id)
            .map(device -> {
                device.setImei(request.imei());
                device.setModel(request.model());
                device.setSimNumber(request.simNumber());
                if (request.status() != null) {
                    device.setStatus(Device.Status.valueOf(request.status()));
                }
                if (request.customerId() != null) {
                    customerRepository.findById(request.customerId()).ifPresent(customer -> {
                        device.setCustomer(customer);
                        device.setStatus(Device.Status.ASSIGNED);
                        device.setAssignedAt(Instant.now());
                    });
                } else if (request.status() != null && request.status().equals("AVAILABLE")) {
                    device.setCustomer(null);
                    device.setAssignedAt(null);
                }
                return ResponseEntity.ok(DeviceResponse.from(repository.save(device)));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record DeviceRequest(
        String imei,
        String model,
        String simNumber,
        Long customerId,
        String status
    ) {}

    public record DeviceResponse(
        Long id,
        String imei,
        String model,
        String sim_number,
        Long customer_id,
        String customer_name,
        String status,
        String assigned_at,
        String created_at
    ) {
        public static DeviceResponse from(Device device) {
            return new DeviceResponse(
                device.getId(),
                device.getImei(),
                device.getModel(),
                device.getSimNumber(),
                device.getCustomer() != null ? device.getCustomer().getId() : null,
                device.getCustomer() != null ? device.getCustomer().getFirstName() + " " + device.getCustomer().getLastName() : null,
                device.getStatus().name(),
                device.getAssignedAt() != null ? device.getAssignedAt().toString() : null,
                device.getCreatedAt() != null ? device.getCreatedAt().toString() : null
            );
        }
    }
}
