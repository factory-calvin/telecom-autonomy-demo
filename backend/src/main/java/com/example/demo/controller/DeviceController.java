package com.example.demo.controller;

import com.example.demo.model.Device;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceRepository repository;
    private final CustomerRepository customerRepository;

    public DeviceController(DeviceRepository repository, CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<DeviceResponse> getAllDevices() {
        log.info("Fetching all devices");
        List<DeviceResponse> devices = repository.findAll().stream().map(DeviceResponse::from).toList();
        log.debug("Found {} devices", devices.size());
        return devices;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable Long id) {
        log.info("Fetching device with id={}", id);
        return repository.findById(id).map(device -> {
            log.debug("Found device: imei={}", device.getImei());
            return ResponseEntity.ok(DeviceResponse.from(device));
        }).orElseGet(() -> {
            log.warn("Device not found: id={}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @PostMapping
    public DeviceResponse createDevice(@RequestBody DeviceRequest request) {
        log.info("Creating device: imei={}, model={}", request.imei(), request.model());
        Device device = new Device(request.imei(), request.model(), request.simNumber());
        if (request.customerId() != null) {
            customerRepository.findById(request.customerId()).ifPresent(customer -> {
                device.setCustomer(customer);
                device.setStatus(Device.Status.ASSIGNED);
                device.setAssignedAt(Instant.now());
                log.debug("Device assigned to customer: customerId={}", customer.getId());
            });
        }
        Device saved = repository.save(device);
        log.info("Device created: id={}, imei={}", saved.getId(), saved.getImei());
        return DeviceResponse.from(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id, @RequestBody DeviceRequest request) {
        log.info("Updating device: id={}", id);
        return repository.findById(id).map(device -> {
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
            Device saved = repository.save(device);
            log.info("Device updated: id={}, imei={}", saved.getId(), saved.getImei());
            return ResponseEntity.ok(DeviceResponse.from(saved));
        }).orElseGet(() -> {
            log.warn("Device not found for update: id={}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        log.info("Deleting device: id={}", id);
        if (!repository.existsById(id)) {
            log.warn("Device not found for deletion: id={}", id);
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        log.info("Device deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }

    public record DeviceRequest(String imei, String model, String simNumber, Long customerId, String status) {
    }

    public record DeviceResponse(Long id, String imei, String model, String sim_number, Long customer_id,
            String customer_name, String status, String assigned_at, String created_at) {
        public static DeviceResponse from(Device device) {
            return new DeviceResponse(device.getId(), device.getImei(), device.getModel(), device.getSimNumber(),
                    device.getCustomer() != null ? device.getCustomer().getId() : null,
                    device.getCustomer() != null
                            ? device.getCustomer().getFirstName() + " " + device.getCustomer().getLastName()
                            : null,
                    device.getStatus().name(),
                    device.getAssignedAt() != null ? device.getAssignedAt().toString() : null,
                    device.getCreatedAt() != null ? device.getCreatedAt().toString() : null);
        }
    }
}
