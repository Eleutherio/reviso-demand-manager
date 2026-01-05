package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Client;
import com.guilherme.reviso_demand_manager.infra.ClientRepository;
import com.guilherme.reviso_demand_manager.web.ClientDTO;
import com.guilherme.reviso_demand_manager.web.CreateClientDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public ClientDTO createClient(CreateClientDTO dto) {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setName(dto.name());
        client.setSegment(dto.segment());
        client.setActive(true);
        client.setCreatedAt(LocalDateTime.now());

        Client saved = clientRepository.save(client);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ClientDTO> getAllClients() {
        return clientRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private ClientDTO toDTO(Client client) {
        return new ClientDTO(
                client.getId(),
                client.getName(),
                client.getSegment(),
                client.getActive(),
                client.getCreatedAt()
        );
    }
}
