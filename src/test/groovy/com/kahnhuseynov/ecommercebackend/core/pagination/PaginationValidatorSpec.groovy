package com.kahnhuseynov.ecommercebackend.core.pagination

import com.kahnhuseynov.ecommercebackend.core.exception.InvalidRequestException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import spock.lang.Specification

class PaginationValidatorSpec extends Specification {

    def "normalizes frontend snake case sort field"() {
        given:
        def pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "created_at"))

        when:
        def result = PaginationValidator.normalizeSort(
                pageable,
                [created_at: "createdAt", createdAt: "createdAt"]
        )

        then:
        result.pageNumber == 1
        result.pageSize == 10
        result.sort.getOrderFor("createdAt").descending
    }

    def "rejects unsupported sort field"() {
        when:
        PaginationValidator.normalizeSort(
                PageRequest.of(0, 20, Sort.by("unsupported")),
                [name: "name"]
        )

        then:
        def exception = thrown(InvalidRequestException)
        exception.message.contains("Unsupported sort field 'unsupported'")
    }
}
