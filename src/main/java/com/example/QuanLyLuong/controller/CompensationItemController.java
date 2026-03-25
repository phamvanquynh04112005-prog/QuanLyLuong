package com.example.QuanLyLuong.controller;

import java.time.LocalDate;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.example.QuanLyLuong.common.CompensationItemType;
import com.example.QuanLyLuong.entity.CompensationItem;
import com.example.QuanLyLuong.service.CompensationItemService;
import com.example.QuanLyLuong.service.EmployeeService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/compensation-items")
@RequiredArgsConstructor
public class CompensationItemController {

    private final CompensationItemService compensationItemService;
    private final EmployeeService employeeService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword, Model model) {
        List<CompensationItem> items = compensationItemService.findAll(keyword);
        model.addAttribute("items", items);
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("types", CompensationItemType.values());
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("pageTitle", "Ph\u1ee5 c\u1ea5p v\u00e0 th\u01b0\u1edfng");
        model.addAttribute("contentTemplate", "compensation-item/list");
        return "layout/base";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long employeeId,
                       @RequestParam CompensationItemType componentType,
                       @RequestParam String name,
                       @RequestParam Double amount,
                       @RequestParam(required = false) Boolean taxable,
                       @RequestParam(required = false) Boolean recurring,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(required = false) LocalDate effectiveDate,
                       @RequestParam(required = false) LocalDate endDate,
                       @RequestParam(required = false) String note,
                       @RequestParam(required = false) String keyword,
                       RedirectAttributes redirectAttributes) {
        compensationItemService.save(
                employeeId,
                componentType,
                name,
                amount,
                taxable,
                recurring,
                active,
                effectiveDate,
                endDate,
                note
        );
        redirectAttributes.addFlashAttribute("successMsg", "\u0110\u00e3 l\u01b0u c\u1ea5u ph\u1ea7n thu nh\u1eadp/kh\u1ea5u tr\u1eeb \u0111\u1ed9ng.");
        return buildRedirectByKeyword(keyword);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String keyword,
                         RedirectAttributes redirectAttributes) {
        compensationItemService.delete(id);
        redirectAttributes.addFlashAttribute("successMsg", "\u0110\u00e3 x\u00f3a c\u1ea5u ph\u1ea7n \u0111\u1ed9ng.");
        return buildRedirectByKeyword(keyword);
    }

    private String buildRedirectByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "redirect:/compensation-items";
        }
        return "redirect:/compensation-items?keyword=" + URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
    }
}
