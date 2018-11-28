/*
 * Copyright (c) 2018 Leonardo Pessoa
 * https://github.com/lmpessoa/java-services
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lmpessoa.services.internal.validating;

import static javax.validation.ConstraintTarget.IMPLICIT;
import static javax.validation.ConstraintTarget.RETURN_VALUE;
import static javax.validation.constraintvalidation.ValidationTarget.ANNOTATED_ELEMENT;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.validation.Constraint;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ReportAsSingleViolation;
import javax.validation.UnexpectedTypeException;
import javax.validation.ValidationException;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern.Flag;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;
import javax.validation.groups.Default;

import com.lmpessoa.services.internal.ClassUtils;
import com.lmpessoa.services.internal.ErrorMessage;
import com.lmpessoa.services.internal.validating.PathNode.CrossParameterPathNode;

final class ConstraintAnnotation {

   private static final String GROUPS = "groups";
   private static final String VALUE = "value";

   private static Clock clock = Clock.systemDefaultZone();

   private final Map<String, Object> attributes;
   private final AnnotatedElement parent;
   private final Annotation annotation;

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();
      result.append('@');
      result.append(annotation.annotationType().getName());
      StringBuilder params = new StringBuilder();
      for (Entry<String, Object> entry : attributes.entrySet()) {
         String key = entry.getKey();
         if (!GROUPS.equals(key) && !"payload".equals(key)) {
            params.append(key);
            params.append('=');
            params.append(entry.getValue());
            params.append(',');
         }
      }
      if (params.length() > 0) {
         params.delete(params.length() - 1, params.length());
         result.append('(');
         result.append(params);
         result.append(')');
      }
      return result.toString();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof ConstraintAnnotation) {
         return annotation.equals(((ConstraintAnnotation) obj).annotation);
      } else if (obj instanceof Annotation) {
         return annotation.equals(obj);
      }
      return false;
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   static Object unwrapOptional(Object value) {
      if (value instanceof Optional) {
         return ((Optional<?>) value).orElse(null);
      } else if (value instanceof OptionalInt) {
         OptionalInt opt = (OptionalInt) value;
         return opt.isPresent() ? opt.getAsInt() : null;
      } else if (value instanceof OptionalLong) {
         OptionalLong opt = (OptionalLong) value;
         return opt.isPresent() ? opt.getAsLong() : null;
      } else if (value instanceof OptionalDouble) {
         OptionalDouble opt = (OptionalDouble) value;
         return opt.isPresent() ? opt.getAsDouble() : null;
      }
      return value;
   }

   static void setClock(Clock clock) {
      ConstraintAnnotation.clock = Objects.requireNonNull(clock);
   }

   static Set<ConstraintAnnotation> of(AnnotatedElement element) {
      Set<ConstraintAnnotation> result = new HashSet<>();
      for (Annotation ann : element.getAnnotations()) {
         Method method = ClassUtils.getMethod(ann.annotationType(), VALUE);
         if (method != null && method.getReturnType().isArray()
                  && method.getReturnType().getComponentType().isAnnotationPresent(
                           Constraint.class)) {
            try {
               Annotation[] list = (Annotation[]) method.invoke(ann);
               Arrays.asList(list).stream().map(a -> new ConstraintAnnotation(element, a)).forEach(
                        result::add);
            } catch (IllegalAccessException | InvocationTargetException e) {
               throw new ConstraintDefinitionException(e);
            }
         } else if (ann.annotationType().isAnnotationPresent(Constraint.class)) {
            result.add(new ConstraintAnnotation(element, ann));
         }
      }
      return Collections.unmodifiableSet(result);
   }

   Annotation getAnnotation() {
      return annotation;
   }

   Set<Class<?>> getGroups() {
      Class<?>[] groups = assertAttribute(GROUPS, Class[].class);
      if (groups == null || groups.length == 0) {
         groups = new Class<?>[] { Default.class };
      }
      return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(groups)));
   }

   String getMessage() {
      String message = assertAttribute("message", String.class);
      if (message == null || message.isEmpty()) {
         message = String.format("{%s.message}", annotation.annotationType().getName());
      }
      return message;
   }

   ConstraintTarget getValidationAppliesTo() {
      ConstraintTarget target = assertAttribute("validationAppliesTo", ConstraintTarget.class);
      if (target == null) {
         target = IMPLICIT;
      }
      if (target == IMPLICIT && parent instanceof Executable) {
         Executable exec = (Executable) parent;
         if (exec.getParameterCount() == 0) {
            target = RETURN_VALUE;
         } else if (exec instanceof Method && ((Method) exec).getReturnType() == void.class) {
            target = ConstraintTarget.PARAMETERS;
         } else {
            throw new ConstraintDefinitionException(ErrorMessage.AMBIGUOUS_TARGET.get());
         }
      }
      return target;
   }

   Object get(String propertyName) {
      return attributes.get(propertyName);
   }

   <T> T get(String propertyName, Class<T> clazz) {
      return assertAttribute(propertyName, clazz);
   }

   Map<String, Object> getAttributes() {
      Map<String, Object> result = new HashMap<>();
      for (Entry<String, Object> entry : attributes.entrySet()) {
         if (!isDefinitionVariable(entry.getKey())) {
            result.put(entry.getKey(), entry.getValue());
         }
      }
      return Collections.unmodifiableMap(result);
   }

   Set<Violation> validate(PathNode entry) {
      Object value = entry.getValue();
      Constraint constraint = annotation.annotationType().getAnnotation(Constraint.class);
      if (constraint == null) {
         throw new ConstraintDefinitionException();
      }
      if (Min.class.getPackage().equals(annotation.annotationType().getPackage())) {
         if (!isValid(value)) {
            return Collections.singleton(new Violation(this, entry));
         }
      } else {
         return validateConstraint(constraint, entry);
      }
      return Collections.emptySet();
   }

   private ConstraintAnnotation(AnnotatedElement parent, Annotation annotation) {
      this.parent = parent;
      this.annotation = annotation;
      Map<String, Object> attr = new HashMap<>();
      for (Method method : annotation.annotationType().getMethods()) {
         if (method.getDeclaringClass() != Annotation.class) {
            try {
               Object value = method.invoke(annotation);
               attr.put(method.getName(), value);
            } catch (IllegalAccessException | InvocationTargetException e) {
               throw new ConstraintDefinitionException(e);
            }
         }
      }
      this.attributes = Collections.unmodifiableMap(attr);
   }

   private static boolean isDefinitionVariable(String name) {
      return Arrays.asList(GROUPS, "payload", "message", "validationAppliesTo").contains(name);
   }

   private Set<Violation> validateConstraint(Constraint constraintAnnotation, PathNode entry) {
      Set<Violation> result = new HashSet<>();
      Set<ConstraintAnnotation> nested = of(annotation.annotationType());
      Class<? extends ConstraintValidator<?, ?>>[] validators = constraintAnnotation.validatedBy();
      if (validators.length == 0 && nested.isEmpty()) {
         throw new ConstraintDefinitionException();
      }
      for (ConstraintAnnotation ann : nested) {
         ann.validate(entry).forEach(result::add);
      }
      if (entry.getValue() != null) {
         ConstraintValidator<Annotation, Object> validator = findValidator(
                  entry.getValue().getClass(), validators,
                  entry instanceof CrossParameterPathNode ? ValidationTarget.PARAMETERS
                           : ANNOTATED_ELEMENT);
         if (validator != null) {
            ValidationContext context = new ValidationContext(this, entry, clock);
            if (!validator.isValid(entry.getValue(), context)) {
               context.getViolations().forEach(result::add);
            }
         } else if (nested.isEmpty()) {
            throw new UnexpectedTypeException();
         }
      }
      if (result.isEmpty()) {
         return Collections.emptySet();
      }
      if (annotation.annotationType().isAnnotationPresent(ReportAsSingleViolation.class)) {
         return Collections.singleton(new Violation(this, entry));
      }
      return Collections.unmodifiableSet(result);
   }

   private ConstraintValidator<Annotation, Object> findValidator(Class<?> valueClass,
      Class<? extends ConstraintValidator<?, ?>>[] validators, ValidationTarget target) {
      Method foundMethod = null;
      Class<? extends ConstraintValidator<?, ?>> foundValidator = null;
      for (Class<? extends ConstraintValidator<?, ?>> validatorClass : validators) {
         Method[] methods = ClassUtils.findMethods(validatorClass,
                  m -> "isValid".equals(m.getName()) && !m.isSynthetic()
                           && m.getParameterTypes()[0].isAssignableFrom(valueClass)
                           && m.getParameterTypes()[1] == ConstraintValidatorContext.class);
         for (Method method : methods) {
            if (foundMethod != null
                     && foundMethod.getParameterTypes()[0]
                              .isAssignableFrom(method.getParameterTypes()[0])
                     && !method.getParameterTypes()[0]
                              .isAssignableFrom(foundMethod.getParameterTypes()[0])) {
               throw new UnexpectedTypeException();
            }
            SupportedValidationTarget validationTarget = validatorClass
                     .getAnnotation(SupportedValidationTarget.class);
            List<ValidationTarget> targets = validationTarget != null
                     ? Arrays.asList(validationTarget.value())
                     : Arrays.asList(ANNOTATED_ELEMENT);
            Class<?> param = method.getParameterTypes()[0];
            if (target != ValidationTarget.PARAMETERS || param == Object.class
                     || param == Object[].class && targets.contains(target)) {
               foundValidator = validatorClass;
               foundMethod = method;
            }
         }
      }
      if (foundValidator == null) {
         return null;
      }
      try {
         @SuppressWarnings("unchecked")
         ConstraintValidator<Annotation, Object> result = (ConstraintValidator<Annotation, Object>) foundValidator
                  .newInstance();
         result.initialize(annotation);
         return result;
      } catch (IllegalArgumentException e) {
         throw new ConstraintDefinitionException(e);
      } catch (InstantiationException | IllegalAccessException e) {
         throw new ValidationException(e);
      }
   }

   private boolean isValid(Object value) {
      value = unwrapOptional(value);
      if (value == null) {
         return !(annotation instanceof NotNull || annotation instanceof NotEmpty
                  || annotation instanceof NotBlank);
      } else if (annotation instanceof Null) {
         return false;
      } else if (annotation instanceof NotNull) {
         return true;
      }
      switch (annotation.annotationType().getSimpleName()) {
         case "AssertTrue":
            return isValidAssertTrue(value);
         case "AssertFalse":
            return isValidAssertFalse(value);
         case "Min":
            return isValidMin(value);
         case "Max":
            return isValidMax(value);
         case "DecimalMin":
            return isValidDecimalMin(value);
         case "DecimalMax":
            return isValidDecimalMax(value);
         case "Negative":
            return isValidNegative(value);
         case "NegativeOrZero":
            return isValidNegativeOrZero(value);
         case "Positive":
            return isValidPositive(value);
         case "PositiveOrZero":
            return isValidPositiveOrZero(value);
         case "Size":
            return isValidSize(value);
         case "Digits":
            return isValidDigits(value);
         case "Past":
            return isValidPast(value);
         case "PastOrPresent":
            return isValidPastOrPresent(value);
         case "Future":
            return isValidFuture(value);
         case "FutureOrPresent":
            return isValidFutureOrPresent(value);
         case "Pattern":
            return isValidPattern(value);
         case "NotEmpty":
            return isValidNotEmpty(value);
         case "NotBlank":
            return isValidNotBlank(value);
         case "Email":
            return isValidEmail(value);
         default:
            throw new ConstraintDefinitionException(
                     ErrorMessage.UNKNOWN_CONSTRAINT.with(annotation.annotationType()));
      }
   }

   private boolean isValidAssertTrue(Object value) {
      if (!(value instanceof Boolean)) {
         throw new UnexpectedTypeException();
      }
      return ((Boolean) value).booleanValue();
   }

   private boolean isValidAssertFalse(Object value) {
      return !isValidAssertTrue(value);
   }

   private boolean isValidMin(Object value) {
      BigDecimal min = BigDecimal.valueOf((Long) get(VALUE));
      return assertNumber(value, d -> d.compareTo(min) >= 0);
   }

   private boolean isValidMax(Object value) {
      BigDecimal max = BigDecimal.valueOf((Long) get(VALUE));
      return assertNumber(value, d -> d.compareTo(max) <= 0);
   }

   private boolean isValidDecimalMin(Object value) {
      BigDecimal min = new BigDecimal(get(VALUE).toString());
      boolean inclusive = ((Boolean) get("inclusive")).booleanValue();
      return assertNumber(value, d -> d.compareTo(min) > (inclusive ? -1 : 0));
   }

   private boolean isValidDecimalMax(Object value) {
      BigDecimal max = new BigDecimal(get(VALUE).toString());
      boolean inclusive = ((Boolean) get("inclusive")).booleanValue();
      return assertNumber(value, d -> d.compareTo(max) < (inclusive ? 1 : 0));
   }

   private boolean isValidNegative(Object value) {
      return assertNumber(value, d -> d.signum() == -1);
   }

   private boolean isValidNegativeOrZero(Object value) {
      return assertNumber(value, d -> d.signum() <= 0);
   }

   private boolean isValidPositive(Object value) {
      return assertNumber(value, d -> d.signum() == 1);
   }

   private boolean isValidPositiveOrZero(Object value) {
      return assertNumber(value, d -> d.signum() >= 0);
   }

   private boolean isValidSize(Object value) {
      int size;
      if (value instanceof CharSequence) {
         size = ((CharSequence) value).length();
      } else if (value instanceof Collection) {
         size = ((Collection<?>) value).size();
      } else if (value instanceof Map<?, ?>) {
         size = ((Map<?, ?>) value).size();
      } else if (value.getClass().isArray()) {
         size = Array.getLength(value);
      } else {
         throw new UnexpectedTypeException();
      }
      int min = ((Integer) get("min")).intValue();
      int max = ((Integer) get("max")).intValue();
      return size >= min && size <= max;
   }

   private boolean isValidDigits(Object value) {
      int integer = ((Integer) get("integer")).intValue();
      int fraction = ((Integer) get("fraction")).intValue();
      if (value instanceof String || assertNumber(value, d -> true)) {
         String[] str = value.toString().split("\\.", 2);
         return str[0].length() <= integer && (str.length == 1 || str[1].length() <= fraction);
      }
      throw new UnexpectedTypeException();
   }

   private boolean isValidPast(Object value) {
      return assertDateToNow(value) > 0;
   }

   private boolean isValidPastOrPresent(Object value) {
      return assertDateToNow(value) >= 0;
   }

   private boolean isValidFuture(Object value) {
      return assertDateToNow(value) < 0;
   }

   private boolean isValidFutureOrPresent(Object value) {
      return assertDateToNow(value) <= 0;
   }

   private boolean isValidPattern(Object value) {
      if (!(value instanceof CharSequence)) {
         throw new UnexpectedTypeException();
      }
      CharSequence chs = (CharSequence) value;
      Flag[] flags = (Flag[]) get("flags");
      int flagsNum = Arrays.stream(flags).mapToInt(Flag::getValue).sum();
      Pattern pattern = Pattern.compile(get("regexp").toString(), flagsNum);
      return pattern.matcher(chs).matches();
   }

   private boolean isValidNotEmpty(Object value) {
      if (value instanceof CharSequence) {
         return ((CharSequence) value).length() > 0;
      } else if (value instanceof Collection) {
         return !((Collection<?>) value).isEmpty();
      } else if (value instanceof Map) {
         return !((Map<?, ?>) value).isEmpty();
      } else if (value.getClass().isArray()) {
         return Array.getLength(value) > 0;
      }
      throw new UnexpectedTypeException();
   }

   private boolean isValidNotBlank(Object value) {
      if (!(value instanceof CharSequence)) {
         throw new UnexpectedTypeException();
      }
      CharSequence chs = (CharSequence) value;
      return !chs.toString().trim().isEmpty();
   }

   private boolean isValidEmail(Object value) {
      if (!(value instanceof CharSequence)) {
         throw new UnexpectedTypeException();
      }
      return Pattern.matches(
               "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08"
                        + "\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-"
                        + "\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
                        + "|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9]"
                        + "[0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|"
                        + "\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])",
               (CharSequence) value);
   }

   private boolean assertNumber(Object value, Predicate<BigDecimal> assertion) {
      if (!(value instanceof BigDecimal || value instanceof BigInteger || value instanceof Byte
               || value instanceof Short || value instanceof Integer || value instanceof Long)) {
         throw new UnexpectedTypeException();
      }
      BigDecimal dvalue = new BigDecimal(value.toString());
      return assertion.test(dvalue);
   }

   private int assertDateToNow(Object value) {
      Instant instant = clock.instant();
      ZonedDateTime now = instant.atZone(clock.getZone());
      if (value instanceof Date) {
         return Date.from(instant).compareTo((Date) value);
      } else if (value instanceof Calendar) {
         Calendar cal = Calendar.getInstance();
         cal.setTime(Date.from(instant));
         return cal.compareTo((Calendar) value);
      } else if (value instanceof Instant) {
         return instant.compareTo((Instant) value);
      } else if (value instanceof LocalDate) {
         return now.toLocalDate().compareTo((LocalDate) value);
      } else if (value instanceof LocalDateTime) {
         return now.toLocalDateTime().compareTo((LocalDateTime) value);
      } else if (value instanceof LocalTime) {
         return now.toLocalTime().compareTo((LocalTime) value);
      } else if (value instanceof MonthDay) {
         MonthDay currentMonthDay = MonthDay.of(now.getMonthValue(), now.getDayOfMonth());
         return currentMonthDay.compareTo((MonthDay) value);
      } else if (value instanceof OffsetDateTime) {
         return now.toOffsetDateTime().compareTo((OffsetDateTime) value);
      } else if (value instanceof OffsetTime) {
         return now.toOffsetDateTime().toOffsetTime().compareTo((OffsetTime) value);
      } else if (value instanceof Year) {
         return Year.of(now.getYear()).compareTo((Year) value);
      } else if (value instanceof YearMonth) {
         YearMonth currentYearMonth = YearMonth.of(now.getYear(), now.getMonthValue());
         return currentYearMonth.compareTo((YearMonth) value);
      } else if (value instanceof ZonedDateTime) {
         return now.compareTo((ZonedDateTime) value);
      } else if (value instanceof HijrahDate) {
         return HijrahDate.from(instant).compareTo((HijrahDate) value);
      } else if (value instanceof JapaneseDate) {
         return JapaneseDate.from(instant).compareTo((JapaneseDate) value);
      } else if (value instanceof MinguoDate) {
         return MinguoDate.from(instant).compareTo((MinguoDate) value);
      } else if (value instanceof ThaiBuddhistDate) {
         return ThaiBuddhistDate.from(instant).compareTo((ThaiBuddhistDate) value);
      }
      throw new UnexpectedTypeException();
   }

   @SuppressWarnings("unchecked")
   private <T> T assertAttribute(String attrName, Class<T> type) {
      if (attributes.containsKey(attrName) && attributes.get(attrName).getClass() != type) {
         throw new ConstraintDefinitionException();
      }
      return (T) attributes.get(attrName);
   }
}
