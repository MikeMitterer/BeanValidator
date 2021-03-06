/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package at.mikemitterer.bv.constraints;


import at.mikemitterer.bv.validator.ValidatePattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Jiren
 */

@Constraint(validator = ValidatePattern.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Pattern {

    String pattern();
    String message();
}
