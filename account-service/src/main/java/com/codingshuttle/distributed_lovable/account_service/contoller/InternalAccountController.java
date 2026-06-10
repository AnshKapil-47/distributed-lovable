package com.codingshuttle.distributed_lovable.account_service.contoller;

import com.codingshuttle.distributed_lovable.account_service.mapper.UserMapper;
import com.codingshuttle.distributed_lovable.account_service.repository.UserRepository;
import com.codingshuttle.distributed_lovable.account_service.service.SubscriptionService;
import com.codingshuttle.distributed_lovable.common_lib.dto.PlanDto;
import com.codingshuttle.distributed_lovable.common_lib.dto.UserDto;
import com.codingshuttle.distributed_lovable.common_lib.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalAccountController {

    public final UserRepository userRepository;
    public final UserMapper userMapper;
    public final SubscriptionService subscriptionService;

    @GetMapping("/users/{id}")
    public UserDto getUserById(@PathVariable Long id){
        return userRepository.findById(id)
                .map(userMapper::toUserDto)
                .orElseThrow(() -> new ResourceNotFoundException("User",id.toString()));
    }

    @GetMapping("/users/by-email")
    public Optional<UserDto> getUserByEmail(@RequestParam String email){
        return userRepository.findByUsernameIgnoreCase(email)
                .map(userMapper::toUserDto);
    }

    @GetMapping("/billing/current-plan")
    public PlanDto getCurrentSubscribedPlan(){
        return subscriptionService.getCurrentSubscribedPlanByUser();
    }
}
