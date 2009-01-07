/*
 * Copyright (c) 2008 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.query.collections.alias;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import com.mysema.query.grammar.types.Expr;
import com.mysema.query.grammar.types.Path;
import com.mysema.query.grammar.types.PathMetadata;

/**
 * AliasFactory provides
 *
 * @author tiwe
 * @version $Id$
 */
public class AliasFactory {
    
    private final ThreadLocal<WeakIdentityHashMap<Object, Expr<?>>> bindings = new ThreadLocal<WeakIdentityHashMap<Object, Expr<?>>>() {
        @Override
        protected WeakIdentityHashMap<Object, Expr<?>> initialValue() {
                return new WeakIdentityHashMap<Object, Expr<?>>();
        }
    };
    
    private final ThreadLocal<Expr<?>> current = new ThreadLocal<Expr<?>>();
    
    public <A> A createAliasForProp(Class<A> cl, Object parent, Expr<?> path){        
        A proxy = createProxy(cl);
        if (!cl.getPackage().getName().equals("java.lang")){
            bindings.get().put(proxy, path);    
        }
        return proxy;
    }
    
    public <A> A createAliasForVar(Class<A> cl, String var){    
        // TODO : cache cl, var pairs
        Expr<?> path = new Path.PSimple<A>(cl, PathMetadata.forVariable(var));
        A proxy = createProxy(cl);
        bindings.get().put(proxy, path);
        return proxy;
    }
    
    @SuppressWarnings("unchecked")
    private <A> A createProxy(Class<A> cl) {
        Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(AliasFactory.class.getClassLoader());
        if (cl.isInterface()){
            enhancer.setInterfaces(new Class[]{cl,ManagedObject.class});
        }else{
            enhancer.setSuperclass(cl);    
            enhancer.setInterfaces(new Class[]{ManagedObject.class});
        }         
        // creates one handler per proxy
        MethodInterceptor handler = new PropertyAccessInvocationHandler(this);
        enhancer.setCallback(handler);
        A rv = (A)enhancer.create();
        return rv;
    }
    
    @SuppressWarnings("unchecked")
    public <A extends Expr<?>> A getCurrent() {
        return (A) current.get();
    }

    public boolean hasCurrent() {
        return current.get() != null;
    }
    
    public Expr<?> pathForAlias(Object key){
        return bindings.get().get(key);
    }

    public void setCurrent(Expr<?> path){
        current.set(path);
    }
    
}
