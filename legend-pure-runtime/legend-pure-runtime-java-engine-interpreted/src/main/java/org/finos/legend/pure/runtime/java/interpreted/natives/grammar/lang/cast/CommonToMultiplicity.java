// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.cast;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public abstract class CommonToMultiplicity extends NativeFunction
{
    protected final ModelRepository repository;

    private final boolean supportsErrorMessage;

    protected CommonToMultiplicity(ModelRepository repository, boolean supportsErrorMessage)
    {
        this.repository = repository;
        this.supportsErrorMessage = supportsErrorMessage;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnMultiplicity = getReturnMultiplicity(params, resolvedMultiplicityParameters, processorSupport);

        CoreInstance param = params.get(0);
        if (Multiplicity.multiplicitiesEqual(returnMultiplicity, Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.multiplicity, processorSupport)))
        {
            return param;
        }

        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(param, M3Properties.values, processorSupport);
        if (!Multiplicity.isValid(returnMultiplicity, values.size()))
        {
            String errorMessage = null;
            if (this.supportsErrorMessage && params.size() >= 2)
            {
                errorMessage = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport).getName();
            }
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), errorMessage != null ? errorMessage : "Cannot cast a collection of size " + values.size() + " to multiplicity " + Multiplicity.print(returnMultiplicity), functionExpressionCallStack);
        }

        CoreInstance result = this.repository.newAnonymousCoreInstance(param.getSourceInformation(), processorSupport.getClassifier(param));
        Instance.addValueToProperty(result, M3Properties.genericType, Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), processorSupport);
        Instance.setValuesForProperty(result, M3Properties.values, values, processorSupport);
        Instance.addValueToProperty(result, M3Properties.multiplicity, returnMultiplicity, processorSupport);

        return result;
    }

    /**
     * Get the multiplicity that should be returned.
     *
     * @return return multiplicity
     */
    protected abstract CoreInstance getReturnMultiplicity(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, ProcessorSupport processorSupport);
}
