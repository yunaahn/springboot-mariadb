package com.example.app.supplement;

import com.example.app.common.ApiException;
import com.example.app.intake.IntakePlanner;
import com.example.app.product.Product;
import com.example.app.product.ProductService;
import com.example.app.supplement.SupplementDtos.CreateRequest;
import com.example.app.supplement.SupplementDtos.CreateResponse;
import com.example.app.supplement.SupplementDtos.Day;
import com.example.app.supplement.SupplementDtos.SupplementResponse;
import com.example.app.supplement.SupplementDtos.UpdateRequest;
import com.example.app.user.User;
import com.example.app.user.UserRepository;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplementService {

    private final UserRepository users;
    private final UserSupplementRepository supplements;
    private final SupplementScheduleRepository schedules;
    private final ProductService products;
    private final IntakePlanner planner;

    @Transactional(readOnly = true)
    public List<SupplementResponse> list(Long userId, SupplementStatus status) {
        return toResponses(status == null
                ? supplements.findByUserIdOrderById(userId)
                : supplements.findByUserIdAndStatusOrderById(userId, status));
    }

    @Transactional(readOnly = true)
    public SupplementResponse get(Long userId, Long id) {
        return toResponses(List.of(getOwned(userId, id))).get(0);
    }

    public CreateResponse create(Long userId, CreateRequest request) {
        User user = lockUser(userId);
        if ((request.productId() == null) == (request.newProduct() == null)) {
            throw ApiException.badRequest("productId와 newProduct 중 하나만 보내 주세요.");
        }
        checkTimes(request.times());
        Product product = request.productId() != null
                ? products.getAccessible(userId, request.productId())
                : products.createByUser(userId, request.newProduct());
        Set<Long> activeProductIds = supplements.findByUserIdAndStatusOrderById(userId, SupplementStatus.ACTIVE)
                .stream().map(UserSupplement::getProductId).collect(Collectors.toSet());

        UserSupplement supplement = new UserSupplement(userId, product.getId(), planner.today(user));
        supplement.setAlias(blankToNull(request.alias()));
        supplement.setDosePerIntake(request.dosePerIntake());
        if (request.mealTiming() != null) {
            supplement.setMealTiming(request.mealTiming());
        }
        supplement.setDaysOfWeek(Day.toMask(request.daysOfWeek()));
        supplement.setTotalQty(request.totalQty());
        supplement.setRemainingQty(request.totalQty());
        supplements.save(supplement);
        request.times().forEach(t -> schedules.save(new SupplementSchedule(supplement.getId(), t)));

        planner.plan(user);
        return new CreateResponse(supplement.getId(), products.duplicateNutrients(product.getId(), activeProductIds));
    }

    public SupplementResponse update(Long userId, Long id, UpdateRequest request) {
        User user = lockUser(userId);
        UserSupplement supplement = getOwned(userId, id);
        if (request.alias() != null) {
            supplement.setAlias(blankToNull(request.alias()));
        }
        if (request.dosePerIntake() != null) {
            supplement.setDosePerIntake(request.dosePerIntake());
        }
        if (request.mealTiming() != null) {
            supplement.setMealTiming(request.mealTiming());
        }
        if (request.totalQty() != null) {
            supplement.setTotalQty(request.totalQty());
            // 수량을 처음 입력하면 남은 수량도 같은 값에서 시작한다.
            if (supplement.getRemainingQty() == null) {
                supplement.setRemainingQty(request.totalQty());
            }
        }
        if (request.remainingQty() != null) {
            supplement.setRemainingQty(request.remainingQty());
        }
        if (request.daysOfWeek() != null) {
            supplement.setDaysOfWeek(Day.toMask(request.daysOfWeek()));
        }
        if (request.times() != null) {
            checkTimes(request.times());
            replaceTimes(id, request.times());
        }
        if (request.daysOfWeek() != null || request.times() != null) {
            planner.clearFuture(supplement);
            planner.plan(user);
        }
        return get(userId, id);
    }

    public SupplementResponse changeStatus(Long userId, Long id, SupplementStatus status) {
        User user = lockUser(userId);
        UserSupplement supplement = getOwned(userId, id);
        if (supplement.getStatus() != status) {
            supplement.changeStatus(status, planner.today(user));
            planner.clearFuture(supplement);
            planner.plan(user);
        }
        return get(userId, id);
    }

    /** 복용 기록까지 삭제한다. 기록을 남기려면 상태를 ENDED로 바꾼다. */
    public void delete(Long userId, Long id) {
        User user = lockUser(userId);
        UserSupplement supplement = getOwned(userId, id);
        planner.deleteAll(user, supplement);
        supplements.delete(supplement);
    }

    private void replaceTimes(Long supplementId, Set<LocalTime> times) {
        Set<LocalTime> missing = new HashSet<>(times);
        for (SupplementSchedule s : schedules.findByUserSupplementIdInOrderByIntakeTime(List.of(supplementId))) {
            s.setActive(missing.remove(s.getIntakeTime()));
        }
        missing.forEach(t -> schedules.save(new SupplementSchedule(supplementId, t)));
    }

    private void checkTimes(Set<LocalTime> times) {
        if (times.stream().anyMatch(t -> t.getSecond() != 0 || t.getNano() != 0)) {
            throw ApiException.badRequest("복용 시각은 HH:mm 형식으로 입력해 주세요.");
        }
    }

    private User lockUser(Long userId) {
        return users.findForUpdate(userId).orElseThrow(ApiException::unauthorized);
    }

    private UserSupplement getOwned(Long userId, Long id) {
        UserSupplement supplement = supplements.findById(id)
                .orElseThrow(() -> ApiException.notFound("영양제를 찾을 수 없어요."));
        if (!supplement.getUserId().equals(userId)) {
            throw ApiException.forbidden();
        }
        return supplement;
    }

    private List<SupplementResponse> toResponses(List<UserSupplement> list) {
        Map<Long, Product> productById = products.findAll(
                list.stream().map(UserSupplement::getProductId).collect(Collectors.toSet()));
        Map<Long, List<LocalTime>> times = schedules
                .findByUserSupplementIdInOrderByIntakeTime(list.stream().map(UserSupplement::getId).toList()).stream()
                .filter(SupplementSchedule::isActive)
                .collect(Collectors.groupingBy(SupplementSchedule::getUserSupplementId,
                        Collectors.mapping(SupplementSchedule::getIntakeTime, Collectors.toList())));
        return list.stream().map(s -> {
            Product p = productById.get(s.getProductId());
            return new SupplementResponse(s.getId(), p.getId(), p.getName(), s.getAlias(), p.getImageUrl(),
                    p.getServingUnit(), s.getDosePerIntake(), s.getMealTiming(), Day.fromMask(s.getDaysOfWeek()),
                    times.getOrDefault(s.getId(), List.of()), s.getTotalQty(), s.getRemainingQty(),
                    s.getStatus(), s.getStartedAt(), s.getEndedAt());
        }).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
