//
// Copyright (C) 2007 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.jvm.untracked;

import gov.nasa.jpf.jvm.ModelAPI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that a field in the model should not be restored when
 * restoring states.
 *
 * Such fields can be used to accumulate information over the whole search
 * (can be useful for coverage statistics etc.).
 *
 * Note that using this feature requires specific versions of Fields, Areas
 * and a special CollapsingRestorer, to be set with the following JPF properties:
 *
 *    +vm.static_area.class = gov.nasa.jpf.jvm.untracked.UntrackedStaticArea
 *    +vm.dynamic_area.class = gov.nasa.jpf.jvm.untracked.UntrackedDynamicArea
 *    +vm.restorer.class = gov.nasa.jpf.jvm.untracked.UntrackedCollapsingRestorer
 *    +vm.fields_factory.class = gov.nasa.jpf.jvm.untracked.UntrackedFieldsFactory
 *    +vm.untracked = true
 *
 * @author Milos Gligoric (milos.gligoric@gmail.com)
 * @author Tihomir Gvero (tihomir.gvero@gmail.com)
 *
 */
@ModelAPI
@Target({ElementType.FIELD})
public @interface UntrackedField { }
