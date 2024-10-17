This folder contains scripts to assist with automating the rollout (and rollback if the need arises) of the BVLS service.

The scripts toggle K8's feature toggles and restarts any affected pods to pick up the secrets changed. There is
nothing sensitive with the actual toggles, they are simple boolean values.

There are two main executable files, a rollout file and a rollback file.  Upon running either file you will be prompted
to provide the environment the process is taking place, dev, preprod or prod.

**_Note: It is advisable to check all the affected service pods before and after running either script. Also keep an eye on any health check endpoints and alerts._**

To rollout execute the script shown below and follow the prompts:

```
./rollout.sh
```

To rollback execute the script shwon below and follow the prompts:

```
./rollback.sh
```
