package com.ubirch.models

import javax.inject.{ Inject, Singleton }

@Singleton
class AcctStoreDAO @Inject() (val events: AcctEventDAO, val owner: AcctEventOwnerDAO)
