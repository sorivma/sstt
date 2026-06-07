package com.sstt.app.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AppPageController {
    @GetMapping("/")
    fun index(): String = "redirect:/tasks"
}
