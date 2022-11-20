package com.greglturnquist.hackingspringboot.reactive.actuator;

import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.aggregation.VariableOperators;
import org.springframework.data.mongodb.core.mapping.Document;

import java.net.URI;

public class ConverterClass {
    /*
    static Converter<Document, HttpTraceWrapper> CONVERTER = new Converter<Document, HttpTraceWrapper>() {
        @Override
        public HttpTraceWrapper convert(Document document) {
            Document httpTrace = document.get("httpTrace", Document.class);
            Document request = document.get("request", Document.class);
            Document response = document.get("response", Document.class);

            return new HttpTraceWrapper(new HttpTrace(
                    new HttpTrace.Request(
                            request.getString("method"),
                            URI.create(request.getString("uri")),
                            request.get("headers", VariableOperators.Map.class),
                            null
                    ),
                    new HttpTrace.Response(
                            response.getInteger("status"),
                            response.get("headers", Map.class)
                    ),
                    httpTrace.getDate("timestamp").toInstant(),
                    null,
                    null,
                    httpTrace.getLong("timeTaken")
            ));
        }
    };
    머 이렇다는데, 메소드가 안나오는 걸 보니 변경사항이 많나보다.
    IDE 가 Converter 를 java8 lambda 로 교체할까 물어보면 거절하라고 한다. Spring data 는 generic parameter 를 기준으로 적절한 convertor 를 찾아 사용하는데
    람다로 바꾸면 java의 type erasure 규칙에 의해 소거되므로 올바로 사용할 수 없게 된다고 한다.
     */
}
