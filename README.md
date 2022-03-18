# qs21 Interpreter for Java

Usage Example:

```
qs21 vm = new qs21();
vm.autoUpdate();

vm.yield("{puts: Hello, World!{nl}}");
```

Example qs21 code for testing:

```
{perf> iter> 10>
  {puts: Hello {iter} {nl}}
  {iter-next}
>}
```
