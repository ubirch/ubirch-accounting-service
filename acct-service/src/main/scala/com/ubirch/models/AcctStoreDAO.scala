package com.ubirch.models

import javax.inject.Inject

class AcctStoreDAO @Inject() (val events: AcctEventDAO, val owner: AcctEventOwnerDAO)
